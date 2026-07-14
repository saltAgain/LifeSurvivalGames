package dev.saltt.survivalgame.lifecycle.game;

import dev.saltt.survivalgame.GameConfig;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gameplay state for one match: teams, who is alive, per-player stats, win condition.
 *
 * Every method runs on the phase thread, so nothing here is synchronised. World access goes
 * through GameWorldHooks, whose MainThreadWorldHook wrapper does the hop to the tick thread.
 */
public final class SurvivalGame {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public interface GameWorldHooks {
        void teleportToSpawns(List<UUID> players);

        void freeze(List<UUID> players, boolean frozen);

        void fillLoot();

        void teleportToDeathmatch(List<UUID> players);

        void broadcast(String message);
    }

    public enum EndReason {
        LAST_TEAM_STANDING,
        TIME_EXPIRED,
        ABANDONED
    }

    private final GameConfig config;
    private final GameWorldHooks world;

    private final Map<UUID, PlayerMatchRecord> records = new LinkedHashMap<>();
    private final Set<UUID> alive = new LinkedHashSet<>();

    private long startedAtMs;
    private long endedAtMs;

    private boolean deathmatch;
    private boolean finished;
    private String winningTeamId;
    private EndReason endReason;

    public SurvivalGame(GameConfig config, GameWorldHooks world) {
        this.config = config;
        this.world = world;
    }

    public void start(List<UUID> roster) {
        startedAtMs = System.currentTimeMillis();
        records.clear();
        alive.clear();

        assignTeams(roster);
        alive.addAll(roster);

        world.fillLoot();
        world.teleportToSpawns(roster);
        world.freeze(roster, true);
        world.broadcast("The games begin! Grace period: " + config.getGraceSeconds() + "s.");

        LOGGER.at(Level.INFO).log("[Game] Started with %d players", alive.size());
    }

    /** Solo when TeamSize is 1, otherwise roster order chunked into teams. */
    private void assignTeams(List<UUID> roster) {
        int size = Math.max(1, config.getTeamSize());
        for (int i = 0; i < roster.size(); i++) {
            UUID uuid = roster.get(i);
            String teamId = size == 1 ? uuid.toString() : "team-" + (i / size);
            records.put(uuid, new PlayerMatchRecord(uuid, teamId, startedAtMs));
        }
    }

    public void endGrace() {
        world.freeze(alivePlayers(), false);
        world.broadcast("Grace period over. Fight!");
    }

    public void beginDeathmatch() {
        if (deathmatch || finished) {
            return;
        }
        deathmatch = true;
        world.teleportToDeathmatch(alivePlayers());
        world.broadcast("Deathmatch! " + alive.size() + " players remain.");
    }

    /** Returns true if this elimination ended the match. */
    public boolean eliminate(UUID uuid, String causeOfDeath, UUID killerUuid) {
        if (finished || !alive.remove(uuid)) {
            return false;
        }

        PlayerMatchRecord record = records.get(uuid);
        if (record != null) {
            record.eliminate(System.currentTimeMillis(), causeOfDeath, killerUuid);
        }

        PlayerMatchRecord killer = killerUuid == null ? null : records.get(killerUuid);
        if (killer != null) {
            killer.getStats().addKill();
        }

        world.broadcast(uuid + " was eliminated. " + alive.size() + " remain.");

        Set<String> teams = aliveTeams();
        if (teams.size() <= 1) {
            finish(teams.isEmpty() ? null : teams.iterator().next(), EndReason.LAST_TEAM_STANDING);
            return true;
        }
        return false;
    }

    public void timeExpired() {
        if (!finished) {
            finish(null, EndReason.TIME_EXPIRED);
        }
    }

    public void abandoned() {
        if (!finished) {
            finish(null, EndReason.ABANDONED);
        }
    }

    private void finish(String winningTeamId, EndReason reason) {
        this.finished = true;
        this.endedAtMs = System.currentTimeMillis();
        this.winningTeamId = winningTeamId;
        this.endReason = reason;

        if (winningTeamId != null) {
            world.broadcast("Winner: " + winningTeamId + "!");
        } else if (reason == EndReason.TIME_EXPIRED) {
            world.broadcast("Time expired -- no winner.");
        }

        LOGGER.at(Level.INFO).log("[Game] Finished (reason=%s, winner=%s)", reason, winningTeamId);
    }

    public void freezeAll(List<UUID> players) {
        world.freeze(players, true);
    }

    public void broadcast(String message) {
        world.broadcast(message);
    }

    private Set<String> aliveTeams() {
        Set<String> teams = new LinkedHashSet<>();
        for (UUID uuid : alive) {
            PlayerMatchRecord record = records.get(uuid);
            if (record != null) {
                teams.add(record.getTeamId());
            }
        }
        return teams;
    }

    public void recordDamage(UUID attacker, UUID victim, long amount) {
        PlayerMatchRecord attackerRecord = attacker == null ? null : records.get(attacker);
        if (attackerRecord != null) {
            attackerRecord.getStats().addDamageDealt(amount);
        }
        PlayerMatchRecord victimRecord = records.get(victim);
        if (victimRecord != null) {
            victimRecord.getStats().addDamageTaken(amount);
        }
    }

    public void recordAssist(UUID uuid) {
        PlayerMatchRecord record = records.get(uuid);
        if (record != null) {
            record.getStats().addAssist();
        }
    }

    public List<PlayerMatchRecord> records() {
        return new ArrayList<>(records.values());
    }

    public long matchEndedAtMs() {
        return endedAtMs > 0 ? endedAtMs : System.currentTimeMillis();
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isAlive(UUID uuid) {
        return alive.contains(uuid);
    }

    public List<UUID> alivePlayers() {
        return new ArrayList<>(alive);
    }

    public String getWinningTeamId() {
        return winningTeamId;
    }

    public EndReason getEndReason() {
        return endReason;
    }
}