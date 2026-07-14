package dev.saltt.survivalgame.lifecycle.data;

import dev.saltt.common.api.GameStatus;
import dev.saltt.common.api.LifeGameType;
import dev.saltt.common.api.ServerRegion;

/**
 * Explicit enum mapping. valueOf(name()) would compile but turn any rename into a runtime
 * failure inside the heartbeat's catch block, i.e. a silent heartbeat outage.
 */
public final class ProtoMapper {

    private ProtoMapper() {
    }

    public static dev.saltt.common.api.proto.GameStatus toProto(GameStatus status) {
        switch (status) {
            case PRE_GAME:
                return dev.saltt.common.api.proto.GameStatus.PRE_GAME;
            case STARTING:
                return dev.saltt.common.api.proto.GameStatus.STARTING;
            case IN_PROGRESS:
                return dev.saltt.common.api.proto.GameStatus.IN_PROGRESS;
            case ENDING:
                return dev.saltt.common.api.proto.GameStatus.ENDING;
            case ENDED:
                return dev.saltt.common.api.proto.GameStatus.ENDED;
            case SERVER_LOADING:
                return dev.saltt.common.api.proto.GameStatus.SERVER_LOADING;
            case SERVER_CLOSING:
                return dev.saltt.common.api.proto.GameStatus.SERVER_CLOSING;
            case SERVER_CRASHING:
                return dev.saltt.common.api.proto.GameStatus.SERVER_CRASHING;
            default:
                throw new IllegalArgumentException("Unmapped GameStatus: " + status);
        }
    }

    public static dev.saltt.common.api.proto.LifeGameType toProto(LifeGameType gameType) {
        switch (gameType) {
            case SURVIVAL_GAMES:
                return dev.saltt.common.api.proto.LifeGameType.SURVIVAL_GAMES;
            default:
                throw new IllegalArgumentException("Unmapped LifeGameType: " + gameType);
        }
    }

    public static dev.saltt.common.api.proto.ServerRegion toProto(ServerRegion region) {
        switch (region) {
            case NORTH_AMERICA:
                return dev.saltt.common.api.proto.ServerRegion.NORTH_AMERICA;
            default:
                throw new IllegalArgumentException("Unmapped ServerRegion: " + region);
        }
    }

    public static LifeGameType fromProto(dev.saltt.common.api.proto.LifeGameType gameType) {
        switch (gameType) {
            case SURVIVAL_GAMES:
                return LifeGameType.SURVIVAL_GAMES;
            default:
                return null;
        }
    }
}