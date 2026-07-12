package dev.saltt.survivalgame.loot;

import javax.annotation.Nonnull;

public record LootStack(@Nonnull String itemId, int quantity) {
}