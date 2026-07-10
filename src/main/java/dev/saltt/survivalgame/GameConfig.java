package dev.saltt.survivalgame;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class GameConfig {

    public static final BuilderCodec<GameConfig> CODEC =
            BuilderCodec.builder(GameConfig.class, GameConfig::new)
                    .append(new KeyedCodec<Short>("MinStartingPlayers", Codec.SHORT),
                            (config, value) -> config.minStartingPlayers = value,
                            (config) -> config.minStartingPlayers)
                    .add()
                    .append(new KeyedCodec<Short>("MaxPlayers", Codec.SHORT),
                            (config, value) -> config.maxPlayers = value,
                            (config) -> config.maxPlayers)
                    .add()
                    .append(new KeyedCodec<String>("GameType", Codec.STRING),
                            (config, value) -> config.fallbackServerIp = value,
                            (config) -> config.fallbackServerIp)
                    .add()
                    .append(new KeyedCodec<Short>("Port", Codec.SHORT),
                            (config, value) -> config.port = value,
                            (config) -> config.port)
                    .add()
                    .append(new KeyedCodec<String>("LifecycleServiceHost", Codec.STRING),
                            (config, value) -> config.lifecycleServiceHost = value,
                            (config) -> config.lifecycleServiceHost)
                    .add()
                    .append(new KeyedCodec<Short>("LifecycleServicePort", Codec.SHORT),
                            (config, value) -> config.lifecycleServicePort = value,
                            (config) -> config.lifecycleServicePort)
                    .add()
                    .append(new KeyedCodec<Short>("HeartbeatIntervalSeconds", Codec.SHORT),
                            (config, value) -> config.heartbeatIntervalSeconds = value,
                            (config) -> config.heartbeatIntervalSeconds)
                    .add()
                    .build();

    private short minStartingPlayers = 2;
    private short maxPlayers = 4;
    private String fallbackServerIp = "172.240.0.94";
    private short port = 5520;

    private String lifecycleServiceHost = "127.0.0.1";
    private short lifecycleServicePort = 9090;
    private short heartbeatIntervalSeconds = 5;

    public GameConfig() {
    }

    public short getMinStartingPlayers() {
        return minStartingPlayers;
    }

    public short getMaxPlayers() {
        return maxPlayers;
    }

    public String getFallbackServerIp() {
        return fallbackServerIp;
    }

    public short getPort() {
        return port;
    }

    public String getLifecycleServiceHost() {
        return lifecycleServiceHost;
    }

    public short getLifecycleServicePort() {
        return lifecycleServicePort;
    }

    public short getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }
}