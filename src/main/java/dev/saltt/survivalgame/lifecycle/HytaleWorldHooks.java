package dev.saltt.survivalgame.lifecycle;

import dev.saltt.survivalgame.lifecycle.game.SurvivalGame;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.List;
import java.util.UUID;

/** The only class that touches the world. Everything here is dispatched onto the tick thread. */
public final class HytaleWorldHooks implements SurvivalGame.GameWorldHooks {

    @Override
    public void teleportToSpawns(List<UUID> players) {
        // TODO: spawn ring from world config, i-th player to i-th pad
    }

    @Override
    public void startFreezeTime(List<UUID> players, boolean frozen) {
        // TODO: movement/damage lock for grace period and podium
    }

    @Override
    public void fillLoot() {
        // TODO: populate chests from loot table
    }

    @Override
    public void teleportToDeathmatch(List<UUID> players) {
        // TODO: teleport survivors to arena
    }

    @Override
    public void broadcast(String message) {
        Universe.get().sendMessage(Message.raw(message));
    }
}