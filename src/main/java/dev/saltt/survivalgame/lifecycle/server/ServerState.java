package dev.saltt.survivalgame.lifecycle.server;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.saltt.common.api.GameStatus;
import dev.saltt.common.api.LifeGameType;
import dev.saltt.common.api.propaties.PlayerManagementProperties;
import dev.saltt.common.api.proto.HeartbeatAck;
import dev.saltt.common.api.proto.HeartbeatServiceGrpc;
import dev.saltt.common.api.proto.SubHeartbeatMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class ServerState {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final UUID matchId;
    private final short maxPlayers;
    private final short minStartingPlayers;
    private final LifeGameType gameType = LifeGameType.SURVIVAL_GAMES;
    private final String fallbackServerIp;
    private final short fallbackServerPort;

    // lifecycle service (heartbeat target)
    private final String lifecycleHost;
    private final int lifecyclePort;
    private final long heartbeatIntervalSeconds;

    private ManagedChannel channel;
    private HeartbeatServiceGrpc.HeartbeatServiceStub heartbeatStub;
    private ScheduledFuture<?> heartbeatTask;

    private GameStatus gameStatus = GameStatus.SERVER_LOADING;

    // concurrent: mutated on the event thread, read on the scheduler thread
    private final Set<UUID> authedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> onlinePlayers = ConcurrentHashMap.newKeySet();

    // phase 1: resettable wait for more players
    private final int countdownSeconds = 10;
    private final int maxCountdownResets = 2;
    private int countdownResets = 0;
    private long countdownStartedAtSeconds = 0;
    private ScheduledFuture<?> countdownTask;

    // phase 2: locked-in start countdown broadcast to chat
    private int startCountdownSeconds;
    private ScheduledFuture<?> startGameTask;

    public ServerState(UUID matchId, short maxPlayers, short minStartingPlayers,
                       String fallbackServerIp, short fallbackServerPort,
                       String lifecycleHost, int lifecyclePort, long heartbeatIntervalSeconds) {
        this.matchId = matchId;
        this.maxPlayers = maxPlayers;
        this.minStartingPlayers = minStartingPlayers;
        this.fallbackServerIp = fallbackServerIp;
        this.fallbackServerPort = fallbackServerPort;
        this.lifecycleHost = lifecycleHost;
        this.lifecyclePort = lifecyclePort;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public boolean setup() {

        //register server message

        return true;
    }

    public void serverReady() {
        gameStatus = GameStatus.PRE_GAME;
        startHeartbeat();
    }

    public void authPlayer(UUID uuid) {
        authedPlayers.add(uuid);
        //drop the reservation if they never actually connect
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> {
                    if (!onlinePlayers.contains(uuid)) {
                        authedPlayers.remove(uuid);
                    }
                },
                PlayerManagementProperties.PLAYER_CONNECT_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
        );
    }

    public void playerJoin(PlayerRef playerRef) {
        if (!authedPlayers.contains(playerRef.getUuid())) {
            playerRef.referToServer(fallbackServerIp, fallbackServerPort);
            return;
        }

        onlinePlayers.add(playerRef.getUuid());

        if (gameStatus == GameStatus.PRE_GAME) {
            updateCountdown();
        }

        heartbeat();
    }

    public void playerLeave(PlayerRef playerRef) {
        onlinePlayers.remove(playerRef.getUuid());

        //cancel the wait if we drop back under the minimum
        if (gameStatus == GameStatus.PRE_GAME && countdownTask != null
                && onlinePlayers.size() < minStartingPlayers) {
            stopCountdown();
        }

        if (onlinePlayers.isEmpty()) {
            //TODO: shutdown maybe?
        }

        heartbeat();
    }

    // ---- phase 1: waiting-for-players countdown ----

    private void updateCountdown() {
        if (onlinePlayers.size() >= maxPlayers) {
            beginStarting();
        } else if (countdownTask == null) {
            if (onlinePlayers.size() >= minStartingPlayers) {
                startCountdown();
            }
        } else if (shouldCountdownRestart()) {
            startCountdown();
        }
    }

    private void startCountdown() {
        stopCountdown();
        countdownStartedAtSeconds = nowSeconds();
        countdownTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(
                this::beginStarting, countdownSeconds, TimeUnit.SECONDS);
        broadcast("Game starting in " + countdownSeconds + " seconds...");
    }

    private void stopCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel(false);
            countdownTask = null;
        }
    }

    private boolean shouldCountdownRestart() {
        if (countdownResets >= maxCountdownResets) {
            return false;
        }
        //only extend once we're past the halfway point, otherwise there's already plenty of time
        if (nowSeconds() - countdownStartedAtSeconds <= countdownSeconds / 2) {
            return false;
        }
        countdownResets++;
        return true;
    }

    // ---- phase 2: locked-in start countdown ----

    private void beginStarting() {
        if (gameStatus != GameStatus.PRE_GAME) {
            return;
        }
        stopCountdown();

        //lock the roster and let the matchmaker bind players to this started lobby
        authedPlayers.clear();
        authedPlayers.addAll(onlinePlayers);
        gameStatus = GameStatus.STARTING;
        heartbeat();

        startCountdownSeconds = 10;
        startGameTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::tickStartCountdown, 0, 1, TimeUnit.SECONDS);
    }

    private void tickStartCountdown() {
        if (startCountdownSeconds > 0) {
            broadcast("Game starting in " + startCountdownSeconds + "...");
            startCountdownSeconds--;
            return;
        }
        stopStartCountdown();
        startGame();
    }

    private void stopStartCountdown() {
        if (startGameTask != null) {
            startGameTask.cancel(false);
            startGameTask = null;
        }
    }

    private void startGame() {
        //TODO: tp to spawn points, begin gameplay
    }

    private void broadcast(String text) {
        Universe.get().sendMessage(Message.raw(text));
    }

    private static long nowSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    // ---- heartbeat ----

    private void startHeartbeat() {
        this.channel = ManagedChannelBuilder.forAddress(lifecycleHost, lifecyclePort)
                .usePlaintext()
                .build();
        this.heartbeatStub = HeartbeatServiceGrpc.newStub(channel);

        heartbeatTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::heartbeat,
                1,                          // initial delay
                heartbeatIntervalSeconds,   // period
                TimeUnit.SECONDS
        );

        LOGGER.at(Level.INFO).log("[Heartbeat] Started -> %s:%d every %ds",
                lifecycleHost, lifecyclePort, heartbeatIntervalSeconds);
    }

    private void heartbeat() {
        try {
            SubHeartbeatMessage message = buildHeartbeatMessage();

            // async stub -> returns immediately, never blocks the scheduler thread
            heartbeatStub.heartbeat(message, new StreamObserver<>() {
                @Override
                public void onNext(HeartbeatAck ack) {
                    if (!ack.getAcknowledged()) {
                        LOGGER.at(Level.WARNING).log("[Heartbeat] Not acknowledged by lifecycle service");
                    }
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.at(Level.WARNING).log("[Heartbeat] Send failed: %s", t.getMessage());
                }

                @Override
                public void onCompleted() {
                    // unary call, nothing to do
                }
            });
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("[Heartbeat] Error building/sending heartbeat: %s", e.getMessage());
        }
    }

    private SubHeartbeatMessage buildHeartbeatMessage() {
        return SubHeartbeatMessage.newBuilder()
                .setMatchId(matchId.toString())
                .setGameType(dev.saltt.common.api.proto.LifeGameType.valueOf(gameType.name()))
                .setStatus(dev.saltt.common.api.proto.GameStatus.valueOf(gameStatus.name()))
                .addAllConnectedPlayerIds(toStringList(onlinePlayers))
                .addAllReservedPlayerIds(toStringList(authedPlayers))
                .build();
    }

    private static List<String> toStringList(Set<UUID> uuids) {
        List<String> out = new ArrayList<>(uuids.size());
        for (UUID id : uuids) {
            out.add(id.toString());
        }
        return out;
    }

    public void shutdown() {
        stopCountdown();
        stopStartCountdown();

        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
        if (channel != null) {
            channel.shutdown();
            try {
                channel.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            channel = null;
        }
        LOGGER.at(Level.INFO).log("[Heartbeat] Stopped");
    }
}