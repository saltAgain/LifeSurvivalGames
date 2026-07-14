package dev.saltt.survivalgame.loot.models;

import javax.annotation.Nonnull;

public record LootStack(@Nonnull String itemId, int quantity) {
}