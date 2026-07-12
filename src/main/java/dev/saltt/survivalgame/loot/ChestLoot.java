package dev.saltt.survivalgame.loot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.random.RandomGenerator;

public record ChestLoot(@Nonnull String chestItemId,
                        @Nonnull LootTable initialSpawnTable,
                        @Nullable LootTable respawnTable) {

    public boolean hasRespawnTable() {
        return respawnTable != null;
    }

    @Nonnull
    public List<LootStack> randomizedInitialSpawn(@Nonnull RandomGenerator random) {
        return initialSpawnTable.randomized(random);
    }

    /** Empty when the chest is configured not to refill. */
    @Nonnull
    public List<LootStack> randomizedRespawn(@Nonnull RandomGenerator random) {
        return respawnTable == null ? List.of() : respawnTable.randomized(random);
    }
}