package dev.saltt.survivalgame.lifecycle.server;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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
        //remove player if they dont join after timeout
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
        if(!authedPlayers.contains(playerRef.getUuid())) {
            //not authed
            playerRef.referToServer(fallbackServerIp, fallbackServerPort);
        }

        onlinePlayers.add(playerRef.getUuid());

        if(onlinePlayers.size() >= minStartingPlayers) {
            //
        }

        heartbeat();
    }

    public void playerLeave(PlayerRef playerRef) {
        onlinePlayers.remove(playerRef.getUuid());
        if(onlinePlayers.size() == 0) {
            //TODO: shutdown maybe?
        }
    }

    // heartbeat

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