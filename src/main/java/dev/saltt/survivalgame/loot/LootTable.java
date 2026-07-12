package dev.saltt.survivalgame.loot;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.random.RandomGenerator;

public record LootTable(@Nonnull String tableId, @Nonnull List<LootEntry> entries) {

    public LootTable {
        entries = List.copyOf(entries);
    }

    /** Resolves every entry's quantity pool down to a single concrete amount. */
    @Nonnull
    public List<LootStack> randomized(@Nonnull RandomGenerator random) {
        return entries.stream()
                .map(entry -> new LootStack(entry.itemId(), entry.rollQuantity(random)))
                .toList();
    }
}