package dev.saltt.survivalgame.lifecycle.data;

import dev.saltt.survivalgame.lifecycle.server.PlayerRegistry;
import dev.saltt.common.api.GameStatus;
import dev.saltt.common.api.LifeGameType;
import dev.saltt.common.api.proto.SubHeartbeatMessage;

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
                .setGameType(ProtoMapper.toProto(gameType))
                .setStatus(ProtoMapper.toProto(status))
                .addAllConnectedPlayerIds(registry.onlineIds())
                .addAllAuthedPlayerIds(registry.reservedIds())
                .build();
    }
}