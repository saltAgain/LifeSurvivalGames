package dev.saltt.survivalgame.lifecycle.data;

import dev.saltt.common.api.proto.GameStatus;
import dev.saltt.common.api.proto.LifeGameType;
import dev.saltt.common.api.proto.SubHeartbeatMessage;
import dev.saltt.survivalgame.lifecycle.server.PlayerRegistry;

import java.util.UUID;

public final class HeartbeatMessage {

    private final UUID matchId;
    private final LifeGameType gameType;
    private final PlayerRegistry registry;

    public HeartbeatMessage(UUID matchId, LifeGameType gameType, PlayerRegistry registry) {
        this.matchId = matchId;
        this.gameType = gameType;
        this.registry = registry;
    }

    public SubHeartbeatMessage build(GameStatus status) {
        return SubHeartbeatMessage.newBuilder()
                .setMatchId(matchId.toString())
                .setGameType(gameType)
                .setStatus(status)
                .addAllConnectedPlayerIds(registry.onlineIds())
                .addAllAuthedPlayerIds(registry.reservedIds())
                .build();
    }
}