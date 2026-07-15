package dev.saltt.survivalgame.lifecycle.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Players that have been assigned to the game server
 * Mutated only from the phase thread; read from the heartbeat thread.
 */
public final class PlayerRegistry {

    private final Set<UUID> reserved = ConcurrentHashMap.newKeySet();
    private final Set<UUID> online = ConcurrentHashMap.newKeySet();

    private volatile boolean locked = false;

    public void reserve(UUID uuid) {
        if (locked) {
            return;
        }
        reserved.add(uuid);
    }

    public void expireReservation(UUID uuid) {
        if (locked || online.contains(uuid)) {
            return;
        }
        reserved.remove(uuid);
    }

    public boolean isReserved(UUID uuid) {
        return reserved.contains(uuid);
    }

    public void markOnline(UUID uuid) {
        online.add(uuid);
    }

    public void markOffline(UUID uuid) {
        online.remove(uuid);
        if (!locked) {
            reserved.remove(uuid);
        }
    }

    public boolean isOnline(UUID uuid) {
        return online.contains(uuid);
    }

    /** Freeze the roster to whoever is here now; reservations then exist only for reconnects. */
    public void lockRoster() {
        reserved.clear();
        reserved.addAll(online);
        locked = true;
    }

    /** Roster reopens when a start is aborted, so the matchmaker can refill the lobby. */
    public void unlockRoster() {
        locked = false;
        reserved.clear();
        reserved.addAll(online);
    }

    public boolean isLocked() {
        return locked;
    }

    public int reservedCount() {
        return reserved.size();
    }

    public int onlineCount() {
        return online.size();
    }

    public List<UUID> onlinePlayers() {
        return new ArrayList<>(online);
    }

    public List<String> onlineIds() {
        return toStringList(online);
    }

    public List<String> reservedIds() {
        return toStringList(reserved);
    }

    private static List<String> toStringList(Set<UUID> uuids) {
        if (uuids.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>(uuids.size());
        for (UUID id : uuids) {
            out.add(id.toString());
        }
        return out;
    }
}