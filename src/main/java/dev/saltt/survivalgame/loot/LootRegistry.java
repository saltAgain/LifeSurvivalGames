package dev.saltt.survivalgame.loot;

import dev.saltt.survivalgame.loot.models.ChestLoot;
import dev.saltt.survivalgame.loot.models.LootEntry;
import dev.saltt.survivalgame.loot.models.LootStack;
import dev.saltt.survivalgame.loot.models.LootTable;
import dev.saltt.survivalgame.loot.storage.ChestLootConfig;
import dev.saltt.survivalgame.loot.storage.LootTablesConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

public final class LootRegistry {

    private final Map<String, LootTable> tablesById;
    private final Map<String, ChestLoot> chestsByItemId;

    private LootRegistry(Map<String, LootTable> tablesById, Map<String, ChestLoot> chestsByItemId) {
        this.tablesById = Map.copyOf(tablesById);
        this.chestsByItemId = Map.copyOf(chestsByItemId);
    }

    @Nonnull
    public static LootRegistry build(@Nonnull LootTablesConfig lootTables,
                                     @Nonnull ChestLootConfig chestLoot,
                                     @Nonnull Consumer<String> warn) {

        Map<String, LootTable> tables = new LinkedHashMap<>();

        for (LootTablesConfig.TableDef def : lootTables.getTables()) {
            String tableId = def.getTableId();
            if (tableId == null || tableId.isBlank()) {
                warn.accept("Skipping loot table with a missing TableId.");
                continue;
            }

            List<LootEntry> entries = new ArrayList<>();
            for (LootTablesConfig.EntryDef entryDef : def.getEntries()) {
                String itemId = entryDef.getItemId();
                int[] quantities = entryDef.getQuantities();

                if (itemId == null || itemId.isBlank()) {
                    warn.accept("Table '" + tableId + "' has an entry with no ItemId; skipping.");
                    continue;
                }
                if (quantities == null || quantities.length == 0) {
                    warn.accept("Entry '" + itemId + "' in table '" + tableId
                            + "' has no Quantities; defaulting to [1].");
                    quantities = new int[]{1};
                }
                entries.add(new LootEntry(itemId, quantities));
            }

            if (tables.put(tableId, new LootTable(tableId, entries)) != null) {
                warn.accept("Duplicate TableId '" + tableId + "'; the later definition wins.");
            }
        }

        Map<String, ChestLoot> chests = new HashMap<>();

        for (ChestLootConfig.ChestDef def : chestLoot.getChests()) {
            String chestItemId = def.getChestItemId();
            String initialId = def.getInitialSpawnTable();
            String respawnId = def.getRespawnTable();

            if (chestItemId == null || chestItemId.isBlank()) {
                warn.accept("Skipping chest binding with a missing ChestItemId.");
                continue;
            }

            LootTable initial = initialId == null ? null : tables.get(initialId);
            if (initial == null) {
                warn.accept("Chest '" + chestItemId + "' references unknown InitialSpawnTable '"
                        + initialId + "'; skipping.");
                continue;
            }

            LootTable respawn = null;
            if (respawnId != null && !respawnId.isBlank()) {
                respawn = tables.get(respawnId);
                if (respawn == null) {
                    warn.accept("Chest '" + chestItemId + "' references unknown RespawnTable '"
                            + respawnId + "'; it will not refill.");
                }
            }

            chests.put(chestItemId, new ChestLoot(chestItemId, initial, respawn));
        }

        return new LootRegistry(tables, chests);
    }

    @Nonnull
    public Optional<LootTable> getTable(@Nullable String tableId) {
        return tableId == null ? Optional.empty() : Optional.ofNullable(tablesById.get(tableId));
    }

    @Nonnull
    public Optional<ChestLoot> getChest(@Nullable String chestItemId) {
        return chestItemId == null ? Optional.empty() : Optional.ofNullable(chestsByItemId.get(chestItemId));
    }

    @Nonnull
    public List<LootStack> rollInitialSpawn(@Nullable String chestItemId, @Nonnull RandomGenerator random) {
        return getChest(chestItemId)
                .map(chest -> chest.randomizedInitialSpawn(random))
                .orElse(List.of());
    }

    @Nonnull
    public List<LootStack> rollRespawn(@Nullable String chestItemId, @Nonnull RandomGenerator random) {
        return getChest(chestItemId)
                .map(chest -> chest.randomizedRespawn(random))
                .orElse(List.of());
    }

    public boolean isLootChest(@Nullable String chestItemId) {
        return chestItemId != null && chestsByItemId.containsKey(chestItemId);
    }

    @Nonnull
    public Map<String, LootTable> tables() {
        return tablesById;
    }

    @Nonnull
    public Map<String, ChestLoot> chests() {
        return chestsByItemId;
    }
}