package dev.saltt.survivalgame.lifecycle.game;

/** Combat counters for PvpStatsMessage. Mutated only on the world thread, from combat events. */
public final class PvpStats {

    private int kills;
    private int assists;
    private long damageDealt;
    private long damageTaken;

    public void addKill() {
        kills++;
    }

    public void addAssist() {
        assists++;
    }

    public void addDamageDealt(long amount) {
        damageDealt += amount;
    }

    public void addDamageTaken(long amount) {
        damageTaken += amount;
    }

    public int getKills() {
        return kills;
    }

    public int getAssists() {
        return assists;
    }

    public long getDamageDealt() {
        return damageDealt;
    }

    public long getDamageTaken() {
        return damageTaken;
    }
}