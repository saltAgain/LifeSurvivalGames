package dev.saltt.survivalgame.lifecycle.data;

import dev.saltt.survivalgame.lifecycle.game.PlayerMatchRecord;
import dev.saltt.survivalgame.lifecycle.game.SurvivalGame;
import dev.saltt.survivalgame.lifecycle.server.LifecycleClient;
import dev.saltt.common.api.proto.FlushEnvelope;
import dev.saltt.common.api.proto.PvpStatsMessage;
import dev.saltt.common.api.proto.SurvivalGamesPayload;
import dev.saltt.common.api.proto.SurvivalGamesPlayerFlushMessage;

import java.util.UUID;

public final class MatchFlusher {

    private final UUID matchId;
    private final SurvivalGame game;
    private final LifecycleClient lifecycle;

    public MatchFlusher(UUID matchId, SurvivalGame game, LifecycleClient lifecycle) {
        this.matchId = matchId;
        this.game = game;
        this.lifecycle = lifecycle;
    }

    public void flush() {
        lifecycle.flush(build());
    }

    private FlushEnvelope build() {
        long endedAt = game.matchEndedAtMs();

        SurvivalGamesPayload.Builder payload = SurvivalGamesPayload.newBuilder();
        FlushEnvelope.Builder envelope = FlushEnvelope.newBuilder().setMatchId(matchId.toString());

        for (PlayerMatchRecord record : game.records()) {
            PvpStatsMessage stats = PvpStatsMessage.newBuilder()
                    .setUuid(record.getPlayerUuid().toString())
                    .setKills(record.getStats().getKills())
                    .setAssists(record.getStats().getAssists())
                    .setDamageDealt(record.getStats().getDamageDealt())
                    .setDamageTaken(record.getStats().getDamageTaken())
                    .build();

            SurvivalGamesPlayerFlushMessage.Builder player = SurvivalGamesPlayerFlushMessage.newBuilder()
                    .setPlayerUuid(record.getPlayerUuid().toString())
                    .setMatchId(matchId.toString())
                    .setTeamId(record.getTeamId())
                    .setTimeAlive(record.timeAliveMs(endedAt))
                    .setCauseOfDeath(record.getCauseOfDeath())
                    .setPlayerStats(stats);

            if (record.getKillerUuid() != null) {
                player.setKillerUuid(record.getKillerUuid().toString());
            }

            payload.addPlayers(player.build());
            envelope.addStats(stats);
        }

        return envelope.setSurvivalGamesPayload(payload.build()).build();
    }
}