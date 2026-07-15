package dev.saltt.survivalgame.lifecycle.server;

import dev.saltt.survivalgame.GameConfig;
import dev.saltt.survivalgame.lifecycle.data.MatchFlusher;
import dev.saltt.survivalgame.lifecycle.game.SurvivalGame;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.saltt.common.api.GameStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * PRE_GAME -> STARTING -> IN_PROGRESS -> ENDING -> ENDED -> SERVER_CLOSING,
 * plus SERVER_LOADING at boot and SERVER_CRASHING from anywhere.
 *
 * Everything runs on the single "sg-phase" thread.
 **/
public final class GamePhaseController {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final GameConfig config;
    private final PlayerRegistry registry;
    private final SurvivalGame game;
    private final LifecycleClient lifecycle;
    private final MatchFlusher flusher;
    private final Executor mainThread;

    private final ScheduledExecutorService phase =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "sg-phase");
                thread.setDaemon(true);
                return thread;
            });

    private final Map<UUID, PlayerRef> playerRefs = new ConcurrentHashMap<>();
    private final List<ScheduledFuture<?>> phaseTasks = new ArrayList<>();

    private volatile GameStatus status = GameStatus.SERVER_LOADING;

    private boolean hadPlayers;
    private int countdownResets;
    private long countdownStartedAt;
    private ScheduledFuture<?> waitingCountdownTask;
    private ScheduledFuture<?> emptyLobbyTask;
    private int startCountdownRemaining;

    private Runnable teardownHook = () -> {
    };

    public GamePhaseController(GameConfig config,
                               PlayerRegistry registry,
                               SurvivalGame game,
                               LifecycleClient lifecycle,
                               MatchFlusher flusher,
                               Executor mainThread) {
        this.config = config;
        this.registry = registry;
        this.game = game;
        this.lifecycle = lifecycle;
        this.flusher = flusher;
        this.mainThread = mainThread;
    }

    public void setTeardownHook(Runnable teardownHook) {
        this.teardownHook = teardownHook;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void serverReady() {
        submit(this::enterPreGame);
    }

    /** Decided on the phase thread and answered from the callback, so the gRPC thread never blocks. */
    public CompletableFuture<Boolean> reservePlayer(UUID uuid) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        if (phase.isShutdown()) {
            result.complete(false);
            return result;
        }

        phase.execute(() -> {
            if (status != GameStatus.PRE_GAME
                    || registry.isLocked()
                    || registry.reservedCount() >= config.getMaxPlayers()) {
                result.complete(false);
                return;
            }

            registry.reserve(uuid);
            lifecycle.sendNow();
            result.complete(true);

            // expire if player dosnt connect within 10 seconds
            schedulePhaseTask(() -> {
                if (!registry.isOnline(uuid)) {
                    registry.expireReservation(uuid);
                    lifecycle.sendNow();
                }
            }, config.getPlayerConnectTimeoutMs(), TimeUnit.MILLISECONDS);
        });

        return result;
    }

    public void onJoin(PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        playerRefs.put(uuid, playerRef);
        submit(() -> handleJoin(uuid));
    }

    public void onLeave(PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        playerRefs.remove(uuid);
        submit(() -> handleLeave(uuid));
    }

    public void onPlayerDeath(UUID uuid, String causeOfDeath, UUID killerUuid) {
        submit(() -> {
            if (status == GameStatus.IN_PROGRESS && game.eliminate(uuid, causeOfDeath, killerUuid)) {
                enterEnding();
            }
        });
    }

    public void onDamage(UUID attacker, UUID victim, long amount) {
        submit(() -> {
            if (status == GameStatus.IN_PROGRESS) {
                game.recordDamage(attacker, victim, amount);
            }
        });
    }

    public void onAssist(UUID uuid) {
        submit(() -> {
            if (status == GameStatus.IN_PROGRESS) {
                game.recordAssist(uuid);
            }
        });
    }

    /**
     * Returns immediately. The terminal heartbeat blocks, and crash() is reachable from the
     * world thread via the uncaught-exception handler.
     */
    public void crash(Throwable cause) {
        LOGGER.at(Level.SEVERE).log("[Lifecycle] Crashing: %s", String.valueOf(cause));
        status = GameStatus.SERVER_CRASHING;
        evacuateAll();
        teardownHook.run();
    }

    private void enterPreGame() {
        setStatus(GameStatus.PRE_GAME);
        schedulePhaseTask(() -> {
            if (status == GameStatus.PRE_GAME) {
                LOGGER.at(Level.INFO).log("[Lifecycle] Lobby timed out waiting for players");
                game.broadcast("Not enough players. Sending you back...");
                enterEnded();
            }
        }, config.getLobbyTimeoutSeconds(), TimeUnit.SECONDS);
    }

    private void handleJoin(UUID uuid) {
        if (registry.isLocked()) {
            if (registry.isReserved(uuid)) {
                registry.markOnline(uuid);
                lifecycle.sendNow();
                LOGGER.at(Level.INFO).log("[Lifecycle] %s reconnected", uuid);
            } else {
                refer(uuid);
            }
            return;
        }

        if (!registry.isReserved(uuid)) {
            refer(uuid);
            return;
        }

        registry.markOnline(uuid);
        hadPlayers = true;
        cancelEmptyLobbyTimer();
        lifecycle.sendNow();

        if (status == GameStatus.PRE_GAME) {
            updateWaitingCountdown();
        }
    }

    private void handleLeave(UUID uuid) {
        registry.markOffline(uuid);
        lifecycle.sendNow();

        switch (status) {
            case PRE_GAME:
                if (waitingCountdownTask != null && registry.onlineCount() < config.getMinStartingPlayers()) {
                    abandonWaitingCountdown();
                    game.broadcast("Not enough players. Countdown cancelled.");
                }
                if (registry.onlineCount() == 0 && hadPlayers) {
                    startEmptyLobbyTimer();
                }
                break;

            case STARTING:
                if (registry.onlineCount() < config.getMinStartingPlayers()) {
                    abortStarting();
                }
                break;

            case IN_PROGRESS:
                if (game.eliminate(uuid, "DISCONNECT", null)) {
                    enterEnding();
                } else if (registry.onlineCount() == 0) {
                    game.abandoned();
                    enterEnded();
                }
                break;

            default:
                break;
        }
    }

    private void updateWaitingCountdown() {
        if (registry.onlineCount() >= config.getMaxPlayers()) {
            enterStarting();
            return;
        }

        if (waitingCountdownTask == null) {
            if (registry.onlineCount() >= config.getMinStartingPlayers()) {
                startWaitingCountdown();
            }
            return;
        }

        // a late joiner extends the wait, but capped: otherwise a join/leave cycler holds the
        // lobby open forever, and extending while plenty of time remains is pointless
        if (countdownResets >= config.getMaxCountdownResets()) {
            return;
        }
        if ((nowSeconds() - countdownStartedAt) * 2 <= config.getWaitingCountdownSeconds()) {
            return;
        }
        countdownResets++;
        startWaitingCountdown();
    }

    private void startWaitingCountdown() {
        cancelWaitingCountdown();
        countdownStartedAt = nowSeconds();
        waitingCountdownTask = phase.schedule(
                guarded(this::enterStarting), config.getWaitingCountdownSeconds(), TimeUnit.SECONDS);
        game.broadcast("Game starting in " + config.getWaitingCountdownSeconds() + " seconds...");
    }

    private void cancelWaitingCountdown() {
        if (waitingCountdownTask != null) {
            waitingCountdownTask.cancel(false);
            waitingCountdownTask = null;
        }
    }

    /** Dropped below the minimum, as opposed to superseded by a restart: refund the resets. */
    private void abandonWaitingCountdown() {
        cancelWaitingCountdown();
        countdownResets = 0;
        countdownStartedAt = 0;
    }

    private void startEmptyLobbyTimer() {
        cancelEmptyLobbyTimer();
        emptyLobbyTask = phase.schedule(guarded(() -> {
            if (registry.onlineCount() == 0) {
                LOGGER.at(Level.INFO).log("[Lifecycle] Lobby empty, shutting down");
                enterEnded();
            }
        }), config.getEmptyLobbyTimeoutSeconds(), TimeUnit.SECONDS);
    }

    private void cancelEmptyLobbyTimer() {
        if (emptyLobbyTask != null) {
            emptyLobbyTask.cancel(false);
            emptyLobbyTask = null;
        }
    }

    private void enterStarting() {
        if (status != GameStatus.PRE_GAME) {
            return;
        }
        clearPhaseTasks();

        registry.lockRoster();
        setStatus(GameStatus.STARTING);

        startCountdownRemaining = config.getStartCountdownSeconds();
        phaseTasks.add(phase.scheduleAtFixedRate(
                guarded(this::tickStartCountdown), 0, 1, TimeUnit.SECONDS));
    }

    private void tickStartCountdown() {
        if (status != GameStatus.STARTING) {
            return;
        }
        if (startCountdownRemaining > 0) {
            game.broadcast("Game starting in " + startCountdownRemaining + "...");
            startCountdownRemaining--;
            return;
        }
        enterInProgress();
    }

    /** Fell below the minimum during the start countdown: reopen the roster and go back to waiting. */
    private void abortStarting() {
        clearPhaseTasks();
        registry.unlockRoster();
        countdownResets = 0;
        countdownStartedAt = 0;
        game.broadcast("Not enough players. Start cancelled.");
        enterPreGame();

        if (registry.onlineCount() == 0 && hadPlayers) {
            startEmptyLobbyTimer();
        }
    }

    private void enterInProgress() {
        if (status != GameStatus.STARTING) {
            return;
        }
        clearPhaseTasks();
        setStatus(GameStatus.IN_PROGRESS);

        game.start(registry.onlinePlayers());

        schedulePhaseTask(game::endGrace, config.getGraceSeconds(), TimeUnit.SECONDS);

        int deathmatchAt = Math.max(config.getGraceSeconds() + 1,
                config.getGameDurationSeconds() - config.getDeathmatchSeconds());

        schedulePhaseTask(() -> {
            if (status == GameStatus.IN_PROGRESS) {
                game.beginDeathmatch();
            }
        }, deathmatchAt, TimeUnit.SECONDS);

        schedulePhaseTask(() -> {
            if (status == GameStatus.IN_PROGRESS) {
                game.timeExpired();
                enterEnding();
            }
        }, config.getGameDurationSeconds(), TimeUnit.SECONDS);
    }

    private void enterEnding() {
        if (status != GameStatus.IN_PROGRESS) {
            return;
        }
        clearPhaseTasks();
        setStatus(GameStatus.ENDING);

        game.freezeAll(registry.onlinePlayers());
        flusher.flush();

        schedulePhaseTask(this::enterEnded, config.getEndingSeconds(), TimeUnit.SECONDS);
    }

    private void enterEnded() {
        if (status == GameStatus.ENDED || status == GameStatus.SERVER_CLOSING) {
            return;
        }
        clearPhaseTasks();
        setStatus(GameStatus.ENDED);

        evacuateAll();
        schedulePhaseTask(this::enterClosing, 2, TimeUnit.SECONDS);
    }

    private void enterClosing() {
        if (status == GameStatus.SERVER_CLOSING) {
            return;
        }
        clearPhaseTasks();
        status = GameStatus.SERVER_CLOSING;
        teardownHook.run();
    }

    /** Single choke point for status changes: every transition pushes a heartbeat immediately. */
    private void setStatus(GameStatus next) {
        LOGGER.at(Level.INFO).log("[Lifecycle] %s -> %s (online=%d)",
                status, next, registry.onlineCount());
        this.status = next;
        lifecycle.sendNow();
    }

    private void evacuateAll() {
        for (UUID uuid : new ArrayList<>(playerRefs.keySet())) {
            refer(uuid);
        }
    }

    private void refer(UUID uuid) {
        PlayerRef ref = playerRefs.get(uuid);
        if (ref == null) {
            return;
        }
        mainThread.execute(() ->
                ref.referToServer(config.getFallbackServerIp(), (short) config.getFallbackServerPort()));
    }

    private void submit(Runnable task) {
        if (!phase.isShutdown()) {
            phase.execute(guarded(task));
        }
    }

    private void schedulePhaseTask(Runnable task, long delay, TimeUnit unit) {
        phaseTasks.add(phase.schedule(guarded(task), delay, unit));
    }

    private void clearPhaseTasks() {
        cancelWaitingCountdown();
        cancelEmptyLobbyTimer();
        for (ScheduledFuture<?> task : phaseTasks) {
            task.cancel(false);
        }
        phaseTasks.clear();
    }

    /** An escaping throwable silently kills the executor, so every later countdown would never fire. */
    private Runnable guarded(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                LOGGER.at(Level.SEVERE).log("[Lifecycle] Phase task threw: %s", t);
                crash(t);
            }
        };
    }

    private static long nowSeconds() {
        return System.nanoTime() / 1_000_000_000L;
    }

    public void shutdown() {
        clearPhaseTasks();
        phase.shutdownNow();
    }
}