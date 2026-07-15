package dev.saltt.survivalgame.lifecycle.data.grpc;

import dev.saltt.common.api.proto.LifeGameType;
import dev.saltt.survivalgame.GameConfig;
import dev.saltt.survivalgame.lifecycle.server.GamePhaseController;
import com.hypixel.hytale.logger.HytaleLogger;
import dev.saltt.common.api.proto.RegisterPlayerReserveServiceGrpc;
import dev.saltt.common.api.proto.ReservePlayerAck;
import dev.saltt.common.api.proto.ReservePlayerMessage;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Inbound: the matchmaker calls this to bind a player to this lobby. The reserve decision is
 * made on the phase thread and answered from the callback, so the gRPC thread never blocks.
 */
public final class ReserveService extends RegisterPlayerReserveServiceGrpc.RegisterPlayerReserveServiceImplBase {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final UUID matchId;
    private final LifeGameType gameType;
    private final GameConfig config;
    private final GamePhaseController phases;

    private Server server;

    public ReserveService(UUID matchId, LifeGameType gameType, GameConfig config, GamePhaseController phases) {
        this.matchId = matchId;
        this.gameType = gameType;
        this.config = config;
        this.phases = phases;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(config.getPort())
                .addService(this)
                .build()
                .start();
        LOGGER.at(Level.INFO).log("[Reserve] Listening on :%d", config.getPort());
    }

    @Override
    public void reservePlayer(ReservePlayerMessage request, StreamObserver<ReservePlayerAck> responseObserver) {
        if (!matchId.toString().equals(request.getMatchId())) {
            respond(responseObserver, false);
            return;
        }

        UUID playerId;
        try {
            playerId = UUID.fromString(request.getPlayerId());
        } catch (IllegalArgumentException e) {
            respond(responseObserver, false);
            return;
        }

        phases.reservePlayer(playerId)
                .thenAccept(accepted -> respond(responseObserver, accepted))
                .exceptionally(t -> {
                    respond(responseObserver, false);
                    return null;
                });
    }

    private void respond(StreamObserver<ReservePlayerAck> observer, boolean accepted) {
        observer.onNext(ReservePlayerAck.newBuilder().setAcknowledged(accepted).build());
        observer.onCompleted();
    }

    public void shutdown() {
        if (server == null) {
            return;
        }
        server.shutdown();
        try {
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        } catch (InterruptedException e) {
            server.shutdownNow();
            Thread.currentThread().interrupt();
        }
        server = null;
        LOGGER.at(Level.INFO).log("[Reserve] Stopped");
    }
}