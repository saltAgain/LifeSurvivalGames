package dev.saltt.survivalgame.lifecycle.server;

import dev.saltt.survivalgame.GameConfig;
import com.hypixel.hytale.logger.HytaleLogger;
import dev.saltt.common.api.proto.FlushEnvelope;
import dev.saltt.common.api.proto.HeartbeatAck;
import dev.saltt.common.api.proto.HeartbeatServiceGrpc;
import dev.saltt.common.api.proto.RegisterServerAck;
import dev.saltt.common.api.proto.RegisterServerMessage;
import dev.saltt.common.api.proto.RegisterServerRegistrarServiceGrpc;
import dev.saltt.common.api.proto.SubHeartbeatMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * All outbound RPCs to the lifecycle service Run on its own thread.
 */
public final class LifecycleClient {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final GameConfig config;
    private final Supplier<SubHeartbeatMessage> messageSupplier;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "sg-lifecycle");
                thread.setDaemon(true);
                return thread;
            });

    private ManagedChannel channel;
    private HeartbeatServiceGrpc.HeartbeatServiceStub heartbeatStub;
    private HeartbeatServiceGrpc.HeartbeatServiceBlockingStub heartbeatBlockingStub;
    private RegisterServerRegistrarServiceGrpc.RegisterServerRegistrarServiceBlockingStub registrarStub;

    private ScheduledFuture<?> heartbeatTask;
    private volatile boolean terminated;

    public LifecycleClient(GameConfig config, Supplier<SubHeartbeatMessage> messageSupplier) {
        this.config = config;
        this.messageSupplier = messageSupplier;
    }

    public void connect() {
        this.channel = ManagedChannelBuilder
                .forAddress(config.getLifecycleServiceHost(), config.getLifecycleServicePort())
                .usePlaintext()
                .build();
        this.heartbeatStub = HeartbeatServiceGrpc.newStub(channel);
        this.heartbeatBlockingStub = HeartbeatServiceGrpc.newBlockingStub(channel);
        this.registrarStub = RegisterServerRegistrarServiceGrpc.newBlockingStub(channel);
    }

    /** Registers, retries with backoff, then invokes onRegistered on the lifecycle thread. */
    public void registerServer(RegisterServerMessage message, Runnable onRegistered, Runnable onFailure) {
        executor.execute(() -> {
            for (int attempt = 1; attempt <= config.getRegisterRetryCount(); attempt++) {
                try {
                    RegisterServerAck ack = registrarStub
                            .withDeadlineAfter(config.getRpcTimeoutMs(), TimeUnit.MILLISECONDS)
                            .registerServer(message);
                    if (ack.getAcknowledged()) {
                        LOGGER.at(Level.INFO).log("[Lifecycle] Registered match %s", message.getMatchId());
                        onRegistered.run();
                        return;
                    }
                    LOGGER.at(Level.WARNING).log("[Lifecycle] Registration refused (attempt %d)", attempt);
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("[Lifecycle] Registration failed (attempt %d): %s",
                            attempt, e.getMessage());
                }
                if (!sleep(attempt * 1000L)) {
                    return;
                }
            }
            onFailure.run();
        });
    }

    public void startHeartbeat() {
        heartbeatTask = executor.scheduleAtFixedRate(
                this::sendSafely, 0, config.getHeartbeatIntervalSeconds(), TimeUnit.SECONDS);
        LOGGER.at(Level.INFO).log("[Heartbeat] Started -> %s:%d every %ds",
                config.getLifecycleServiceHost(),
                config.getLifecycleServicePort(),
                config.getHeartbeatIntervalSeconds());
    }

    public void sendNow() {
        if (!terminated) {
            executor.execute(this::sendSafely);
        }
    }

    private void sendSafely() {
        if (terminated || heartbeatStub == null) {
            return;
        }
        try {
            heartbeatStub.heartbeat(messageSupplier.get(), new StreamObserver<HeartbeatAck>() {
                @Override
                public void onNext(HeartbeatAck ack) {
                    if (!ack.getAcknowledged()) {
                        LOGGER.at(Level.WARNING).log("[Heartbeat] Not acknowledged");
                    }
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.at(Level.WARNING).log("[Heartbeat] Send failed: %s", t.getMessage());
                }

                @Override
                public void onCompleted() {
                }
            });
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("[Heartbeat] Error sending heartbeat: %s", e.getMessage());
        }
    }

    public void sendTerminal(SubHeartbeatMessage message) {
        if (terminated || heartbeatBlockingStub == null) {
            return;
        }
        terminated = true;
        try {
            HeartbeatAck ack = heartbeatBlockingStub
                    .withDeadlineAfter(config.getRpcTimeoutMs(), TimeUnit.MILLISECONDS)
                    .heartbeat(message);
            LOGGER.at(Level.INFO).log("[Heartbeat] Terminal sent (status=%s, ack=%s)",
                    message.getStatus(), ack.getAcknowledged());
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("[Heartbeat] Terminal failed: %s", e.getMessage());
        }
    }

    /**
     * TODO: no service is defined for FlushEnvelope in the protos -- wire the stub here once
     * the rpc exists. Must be sent before sendTerminal(), while the channel is still open.
     */
    public void flush(FlushEnvelope envelope) {
        LOGGER.at(Level.INFO).log("[Flush] Envelope ready for match %s (%d stat rows) -- no rpc bound yet",
                envelope.getMatchId(), envelope.getStatsCount());
    }

    public void shutdown() {
        terminated = true;
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
        if (channel != null) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
            channel = null;
        }
        executor.shutdownNow();
        LOGGER.at(Level.INFO).log("[Lifecycle] Client stopped");
    }

    private boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}