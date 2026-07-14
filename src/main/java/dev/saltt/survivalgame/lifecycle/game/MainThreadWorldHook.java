package dev.saltt.survivalgame.lifecycle.game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Hops every world call onto the tick thread. Keeps SurvivalGame single-threaded (phase thread)
 * while the delegate only ever runs where Hytale expects it.
 *
 * Lists are copied because the caller's collection keeps mutating after we hand it off.
 */
public final class MainThreadWorldHook implements SurvivalGame.GameWorldHooks {

    private final SurvivalGame.GameWorldHooks delegate;
    private final Executor mainThread;

    public MainThreadWorldHook(SurvivalGame.GameWorldHooks delegate, Executor mainThread) {
        this.delegate = delegate;
        this.mainThread = mainThread;
    }

    @Override
    public void teleportToSpawns(List<UUID> players) {
        List<UUID> copy = new ArrayList<>(players);
        mainThread.execute(() -> delegate.teleportToSpawns(copy));
    }

    @Override
    public void freeze(List<UUID> players, boolean frozen) {
        List<UUID> copy = new ArrayList<>(players);
        mainThread.execute(() -> delegate.freeze(copy, frozen));
    }

    @Override
    public void fillLoot() {
        mainThread.execute(delegate::fillLoot);
    }

    @Override
    public void teleportToDeathmatch(List<UUID> players) {
        List<UUID> copy = new ArrayList<>(players);
        mainThread.execute(() -> delegate.teleportToDeathmatch(copy));
    }

    @Override
    public void broadcast(String message) {
        mainThread.execute(() -> delegate.broadcast(message));
    }
}