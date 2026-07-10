package dev.saltt.survivalgame;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.BootEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.util.Config;
import dev.saltt.survivalgame.lifecycle.server.ServerState;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static Main instance;


    private ServerState serverState;

    private Config<GameConfig> config;

    public Main(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    protected void setup() {

        config = this.withConfig("gameconfig", GameConfig.CODEC);

        serverState = new ServerState(
                UUID.randomUUID(),
                config.get().getMaxPlayers(),
                config.get().getMinStartingPlayers(),
                config.get().getFallbackServerIp(),
                config.get().getPort(),
                config.get().getLifecycleServiceHost(),
                config.get().getLifecycleServicePort(),
                config.get().getHeartbeatIntervalSeconds());

        getEventRegistry().register(PlayerConnectEvent.class, this::playerJoinEvent);
        getEventRegistry().register(PlayerDisconnectEvent.class, this::playerLeaveEvent);
        getEventRegistry().register(BootEvent.class, this::bootEvent);

        if(!serverState.setup()) {
            LOGGER.at(Level.SEVERE).log("Failed to setup server!");
            LOGGER.at(Level.SEVERE).log("Failed to setup server!");
            LOGGER.at(Level.SEVERE).log("Failed to setup server!");
            LOGGER.at(Level.SEVERE).log("Failed to setup server!");
            LOGGER.at(Level.SEVERE).log("Failed to setup server!");
            LOGGER.at(Level.SEVERE).log("Failed to setup server!");
            LOGGER.at(Level.SEVERE).log("Failed to setup server!");
            LOGGER.at(Level.SEVERE).log("Failed to setup server!");
            this.shutdown();
        }
    }


    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("[Template] Started!");
    }

    private void bootEvent(BootEvent event) {
        serverState.serverReady();

        //start heartbeat

    }

    private void playerJoinEvent(PlayerConnectEvent event) {
        serverState.playerJoin(event.getPlayerRef());
    }

    private void playerLeaveEvent(PlayerDisconnectEvent event) {
        serverState.playerLeave(event.getPlayerRef());
    }

    @Override
    protected void shutdown() {
        serverState.shutdown();
        LOGGER.at(Level.INFO).log("[Template] Shutting down...");
        instance = null;
    }
}