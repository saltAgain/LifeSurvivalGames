package dev.saltt.survivalgame;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import dev.saltt.common.api.ServerRegion;

public class GameConfig {

    public static final BuilderCodec<GameConfig> CODEC =
            BuilderCodec.builder(GameConfig.class, GameConfig::new)
                    .append(new KeyedCodec<>("MinStartingPlayers", Codec.INTEGER),
                            (config, value) -> config.minStartingPlayers = value,
                            (config) -> config.minStartingPlayers)
                    .add()
                    .append(new KeyedCodec<>("MaxPlayers", Codec.INTEGER),
                            (config, value) -> config.maxPlayers = value,
                            (config) -> config.maxPlayers)
                    .add()
                    .append(new KeyedCodec<>("TeamSize", Codec.INTEGER),
                            (config, value) -> config.teamSize = value,
                            (config) -> config.teamSize)
                    .add()
                    .append(new KeyedCodec<>("MapName", Codec.STRING),
                            (config, value) -> config.mapName = value,
                            (config) -> config.mapName)
                    .add()
                    .append(new KeyedCodec<>("Region", Codec.STRING),
                            (config, value) -> config.region = ServerRegion.valueOf(value),
                            (config) -> config.region.name())
                    .add()

                    .append(new KeyedCodec<>("Port", Codec.INTEGER),
                            (config, value) -> config.port = value,
                            (config) -> config.port)
                    .add()
                    .append(new KeyedCodec<>("FallbackServerIp", Codec.STRING),
                            (config, value) -> config.fallbackServerIp = value,
                            (config) -> config.fallbackServerIp)
                    .add()
                    .append(new KeyedCodec<>("FallbackServerPort", Codec.INTEGER),
                            (config, value) -> config.fallbackServerPort = value,
                            (config) -> config.fallbackServerPort)
                    .add()

                    .append(new KeyedCodec<>("LifecycleServiceHost", Codec.STRING),
                            (config, value) -> config.lifecycleServiceHost = value,
                            (config) -> config.lifecycleServiceHost)
                    .add()
                    .append(new KeyedCodec<>("LifecycleServicePort", Codec.INTEGER),
                            (config, value) -> config.lifecycleServicePort = value,
                            (config) -> config.lifecycleServicePort)
                    .add()
                    .append(new KeyedCodec<>("HeartbeatIntervalSeconds", Codec.INTEGER),
                            (config, value) -> config.heartbeatIntervalSeconds = value,
                            (config) -> config.heartbeatIntervalSeconds)
                    .add()
                    .append(new KeyedCodec<>("RpcTimeoutMs", Codec.INTEGER),
                            (config, value) -> config.rpcTimeoutMs = value,
                            (config) -> config.rpcTimeoutMs)
                    .add()
                    .append(new KeyedCodec<>("RegisterRetryCount", Codec.INTEGER),
                            (config, value) -> config.registerRetryCount = value,
                            (config) -> config.registerRetryCount)
                    .add()

                    .append(new KeyedCodec<>("PlayerConnectTimeoutMs", Codec.INTEGER),
                            (config, value) -> config.playerConnectTimeoutMs = value,
                            (config) -> config.playerConnectTimeoutMs)
                    .add()

                    .append(new KeyedCodec<>("WaitingCountdownSeconds", Codec.INTEGER),
                            (config, value) -> config.waitingCountdownSeconds = value,
                            (config) -> config.waitingCountdownSeconds)
                    .add()
                    .append(new KeyedCodec<>("MaxCountdownResets", Codec.INTEGER),
                            (config, value) -> config.maxCountdownResets = value,
                            (config) -> config.maxCountdownResets)
                    .add()
                    .append(new KeyedCodec<>("LobbyTimeoutSeconds", Codec.INTEGER),
                            (config, value) -> config.lobbyTimeoutSeconds = value,
                            (config) -> config.lobbyTimeoutSeconds)
                    .add()
                    .append(new KeyedCodec<>("EmptyLobbyTimeoutSeconds", Codec.INTEGER),
                            (config, value) -> config.emptyLobbyTimeoutSeconds = value,
                            (config) -> config.emptyLobbyTimeoutSeconds)
                    .add()
                    .append(new KeyedCodec<>("StartCountdownSeconds", Codec.INTEGER),
                            (config, value) -> config.startCountdownSeconds = value,
                            (config) -> config.startCountdownSeconds)
                    .add()

                    .append(new KeyedCodec<>("GraceSeconds", Codec.INTEGER),
                            (config, value) -> config.graceSeconds = value,
                            (config) -> config.graceSeconds)
                    .add()
                    .append(new KeyedCodec<>("GameDurationSeconds", Codec.INTEGER),
                            (config, value) -> config.gameDurationSeconds = value,
                            (config) -> config.gameDurationSeconds)
                    .add()
                    .append(new KeyedCodec<>("DeathmatchSeconds", Codec.INTEGER),
                            (config, value) -> config.deathmatchSeconds = value,
                            (config) -> config.deathmatchSeconds)
                    .add()
                    .append(new KeyedCodec<>("EndingSeconds", Codec.INTEGER),
                            (config, value) -> config.endingSeconds = value,
                            (config) -> config.endingSeconds)
                    .add()
                    .build();

    private int minStartingPlayers = 2;
    private int maxPlayers = 4;
    private int teamSize = 1;
    private String mapName = "default";
    private ServerRegion region = ServerRegion.NORTH_AMERICA;

    private int port = 5520;
    private String fallbackServerIp = "172.240.0.94";
    private int fallbackServerPort = 5520;

    private String lifecycleServiceHost = "127.0.0.1";
    private int lifecycleServicePort = 9090;
    private int heartbeatIntervalSeconds = 5;
    private int rpcTimeoutMs = 2_000;
    private int registerRetryCount = 5;

    private int playerConnectTimeoutMs = 30_000;

    private int waitingCountdownSeconds = 30;
    private int maxCountdownResets = 2;
    private int lobbyTimeoutSeconds = 600;
    private int emptyLobbyTimeoutSeconds = 30;
    private int startCountdownSeconds = 10;

    private int graceSeconds = 15;
    private int gameDurationSeconds = 900;
    private int deathmatchSeconds = 120;
    private int endingSeconds = 10;

    public GameConfig() {
    }

    public int getMinStartingPlayers() {
        return minStartingPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public String getMapName() {
        return mapName;
    }

    public ServerRegion getRegion() {
        return region;
    }

    public int getPort() {
        return port;
    }

    public String getFallbackServerIp() {
        return fallbackServerIp;
    }

    public int getFallbackServerPort() {
        return fallbackServerPort;
    }

    public String getLifecycleServiceHost() {
        return lifecycleServiceHost;
    }

    public int getLifecycleServicePort() {
        return lifecycleServicePort;
    }

    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public int getRpcTimeoutMs() {
        return rpcTimeoutMs;
    }

    public int getRegisterRetryCount() {
        return registerRetryCount;
    }

    public int getPlayerConnectTimeoutMs() {
        return playerConnectTimeoutMs;
    }

    public int getWaitingCountdownSeconds() {
        return waitingCountdownSeconds;
    }

    public int getMaxCountdownResets() {
        return maxCountdownResets;
    }

    public int getLobbyTimeoutSeconds() {
        return lobbyTimeoutSeconds;
    }

    public int getEmptyLobbyTimeoutSeconds() {
        return emptyLobbyTimeoutSeconds;
    }

    public int getStartCountdownSeconds() {
        return startCountdownSeconds;
    }

    public int getGraceSeconds() {
        return graceSeconds;
    }

    public int getGameDurationSeconds() {
        return gameDurationSeconds;
    }

    public int getDeathmatchSeconds() {
        return deathmatchSeconds;
    }

    public int getEndingSeconds() {
        return endingSeconds;
    }

    public void validate() {
        if (minStartingPlayers < 1) {
            throw new IllegalStateException("MinStartingPlayers must be >= 1");
        }
        if (maxPlayers < minStartingPlayers) {
            throw new IllegalStateException("MaxPlayers must be >= MinStartingPlayers");
        }
        if (teamSize < 1) {
            throw new IllegalStateException("TeamSize must be >= 1");
        }
        if (waitingCountdownSeconds < 1 || startCountdownSeconds < 1) {
            throw new IllegalStateException("Countdowns must be >= 1 second");
        }
        if (heartbeatIntervalSeconds < 1) {
            throw new IllegalStateException("HeartbeatIntervalSeconds must be >= 1");
        }
        if (gameDurationSeconds <= graceSeconds) {
            throw new IllegalStateException("GameDurationSeconds must exceed GraceSeconds");
        }
        if (outOfRange(port) || outOfRange(fallbackServerPort) || outOfRange(lifecycleServicePort)) {
            throw new IllegalStateException("Ports must be in 1..65535");
        }
    }

    private static boolean outOfRange(int port) {
        return port <= 0 || port > 65535;
    }
}