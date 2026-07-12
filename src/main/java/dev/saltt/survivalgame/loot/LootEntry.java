package dev.saltt.survivalgame.loot;

import javax.annotation.Nonnull;
import java.util.random.RandomGenerator;

public record LootEntry(@Nonnull String itemId, @Nonnull int[] quantities) {

    public LootEntry {
        if (itemId.isBlank()) {
            throw new IllegalArgumentException("LootEntry itemId cannot be blank");
        }
        if (quantities.length == 0) {
            throw new IllegalArgumentException("LootEntry '" + itemId + "' has no quantities");
        }
        quantities = quantities.clone();
    }

    /** Duplicate values in the pool act as weights. */
    public int rollQuantity(@Nonnull RandomGenerator random) {
        return quantities[random.nextInt(quantities.length)];
    }

    @Override
    public int[] quantities() {
        return quantities.clone();
    }
}