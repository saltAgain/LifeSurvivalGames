package dev.saltt.survivalgame.lifecycle.game;

import java.util.UUID;

/** Everything SurvivalGamesPlayerFlushMessage needs about one player. */
public final class PlayerMatchRecord {

    private final UUID playerUuid;
    private final String teamId;
    private final PvpStats stats = new PvpStats();
    private final long joinedAtMs;

    private long eliminatedAtMs = -1;
    private String causeOfDeath = "";
    private UUID killerUuid;

    public PlayerMatchRecord(UUID playerUuid, String teamId, long joinedAtMs) {
        this.playerUuid = playerUuid;
        this.teamId = teamId;
        this.joinedAtMs = joinedAtMs;
    }

    public void eliminate(long atMs, String causeOfDeath, UUID killerUuid) {
        if (eliminatedAtMs >= 0) {
            return;
        }
        this.eliminatedAtMs = atMs;
        this.causeOfDeath = causeOfDeath == null ? "" : causeOfDeath;
        this.killerUuid = killerUuid;
    }

    public long timeAliveMs(long matchEndedAtMs) {
        long end = eliminatedAtMs >= 0 ? eliminatedAtMs : matchEndedAtMs;
        return Math.max(0L, end - joinedAtMs);
    }

    public boolean isEliminated() {
        return eliminatedAtMs >= 0;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getTeamId() {
        return teamId;
    }

    public PvpStats getStats() {
        return stats;
    }

    public String getCauseOfDeath() {
        return causeOfDeath;
    }

    public UUID getKillerUuid() {
        return killerUuid;
    }
}