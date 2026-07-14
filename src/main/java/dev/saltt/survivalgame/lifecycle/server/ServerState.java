package dev.saltt.survivalgame.lifecycle.server;

import dev.saltt.survivalgame.GameConfig;
import dev.saltt.survivalgame.lifecycle.HytaleWorldHooks;
import dev.saltt.survivalgame.lifecycle.data.HeartbeatMessage;
import dev.saltt.survivalgame.lifecycle.data.MatchFlusher;
import dev.saltt.survivalgame.lifecycle.data.ProtoMapper;
import dev.saltt.survivalgame.lifecycle.data.grpc.ReserveService;
import dev.saltt.survivalgame.lifecycle.game.MainThreadWorldHook;
import dev.saltt.survivalgame.lifecycle.game.SurvivalGame;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.saltt.common.api.GameStatus;
import dev.saltt.common.api.LifeGameType;
import dev.saltt.common.api.proto.RegisterServerMessage;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Wiring only. Behaviour lives in GamePhaseController, LifecycleClient, PlayerRegistry,
 * ReserveService and SurvivalGame.
 */
public final class ServerState {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final LifeGameType GAME_TYPE = LifeGameType.SURVIVAL_GAMES;

    private final UUID matchId;
    private final GameConfig config;

    private final PlayerRegistry registry;
    private final SurvivalGame game;
    private final HeartbeatMessage messages;
    private final LifecycleClient lifecycle;
    private final MatchFlusher flusher;
    private final GamePhaseController phases;
    private final ReserveService reserveService;

    private final AtomicBoolean tornDown = new AtomicBoolean(false);

    public ServerState(UUID matchId, GameConfig config) {
        this.matchId = matchId;
        this.config = config;

        // TODO: point this at the real tick executor if HytaleServer exposes one. Universe
        // messages and PlayerRef#referToServer are almost certainly tick-thread-only, and the
        // original code called them from an arbitrary pool thread. One line to change.
        Executor mainThread = HytaleServer.SCHEDULED_EXECUTOR::execute;

        this.registry = new PlayerRegistry();
        this.game = new SurvivalGame(config, new MainThreadWorldHook(new HytaleWorldHooks(), mainThread));
        this.messages = new HeartbeatMessage(matchId, GAME_TYPE, registry);
        this.lifecycle = new LifecycleClient(config, () -> messages.build(getStatus()));
        this.flusher = new MatchFlusher(matchId, game, lifecycle);
        this.phases = new GamePhaseController(config, registry, game, lifecycle, flusher, mainThread);
        this.reserveService = new ReserveService(matchId, GAME_TYPE, config, phases);

        this.phases.setTeardownHook(this::teardown);
    }

    public boolean setup() {
        try {
            config.validate();
        } catch (IllegalStateException e) {
            LOGGER.at(Level.SEVERE).log("[Lifecycle] Invalid config: %s", e.getMessage());
            return false;
        }

        // without this, an uncaught throwable means no status and no heartbeat, and the
        // matchmaker holds our players until the liveness timeout expires
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.at(Level.SEVERE).log("[Lifecycle] Uncaught on %s: %s", thread.getName(), throwable);
            phases.crash(throwable);
        });

        lifecycle.connect();
        lifecycle.startHeartbeat();

        try {
            reserveService.start();
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("[Lifecycle] Could not bind reserve service: %s", e.getMessage());
            return false;
        }
        return true;
    }

    /** World is loaded: register with the lifecycle service, then open the lobby. */
    public void serverReady() {
        lifecycle.registerServer(
                RegisterServerMessage.newBuilder()
                        .setMatchId(matchId.toString())
                        .setGameType(ProtoMapper.toProto(GAME_TYPE))
                        .setMapName(config.getMapName())
                        .setRegion(ProtoMapper.toProto(config.getRegion()))
                        .setMaxPlayers(config.getMaxPlayers())
                        .setMinimumStartingPlayers(config.getMinStartingPlayers())
                        .build(),
                phases::serverReady,
                () -> phases.crash(new IllegalStateException("registration exhausted retries")));
    }

    public void playerJoin(PlayerRef playerRef) {
        phases.onJoin(playerRef);
    }

    public void playerLeave(PlayerRef playerRef) {
        phases.onLeave(playerRef);
    }

    public void playerDeath(UUID uuid, String causeOfDeath, UUID killerUuid) {
        phases.onPlayerDeath(uuid, causeOfDeath, killerUuid);
    }

    public void playerDamaged(UUID attacker, UUID victim, long amount) {
        phases.onDamage(attacker, victim, amount);
    }

    public void playerAssisted(UUID uuid) {
        phases.onAssist(uuid);
    }

    public GameStatus getStatus() {
        return phases.getStatus();
    }

    /**
     * Runs on its own thread: the terminal heartbeat and both channel shutdowns block, and this
     * is reachable from the world thread (plugin disable, or crash via the uncaught handler).
     */
    public void teardown() {
        if (!tornDown.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(() -> {
            lifecycle.sendTerminal(messages.build(getStatus()));
            reserveService.shutdown();
            phases.shutdown();
            lifecycle.shutdown();
            LOGGER.at(Level.INFO).log("[Lifecycle] Teardown complete");
        }, "sg-teardown");
        thread.setDaemon(false);
        thread.start();
    }
}