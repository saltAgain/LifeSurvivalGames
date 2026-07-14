package dev.saltt.survivalgame;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import dev.saltt.survivalgame.loot.*;
import dev.saltt.survivalgame.loot.storage.ChestLootConfig;
import dev.saltt.survivalgame.loot.storage.LootTablesConfig;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;

public class Main extends JavaPlugin {

    // withConfig must run during construction; calling it after setup() throws.
    private final Config<LootTablesConfig> lootTablesConfig =
            this.withConfig("LootTables", LootTablesConfig.CODEC);

    private final Config<ChestLootConfig> chestLootConfig =
            this.withConfig("ChestLoot", ChestLootConfig.CODEC);

    private LootRegistry lootRegistry;
    private ChestLocationStore chestStore;

    public Main(@NonNull JavaPluginInit init) {
        super(init);
    }

    @Override
    public void setup() {
        this.lootTablesConfig.save();
        this.chestLootConfig.save();

        Config<ChestRegistryData> config = this.withConfig("chests", ChestRegistryData.CODEC);
        config.save();
        this.chestStore = new ChestLocationStore(config);

        reloadLoot();
    }

    public void reloadLoot() {
        this.lootRegistry = LootRegistry.build(
                this.lootTablesConfig.get(),
                this.chestLootConfig.get(),
                message -> getLogger().atWarning().log("[loot] " + message)
        );

        getLogger().atInfo().log("[loot] Loaded " + lootRegistry.tables().size()
                + " table(s) and " + lootRegistry.chests().size() + " chest binding(s).");
    }

    @Nonnull
    public LootRegistry getLootRegistry() {
        return lootRegistry;
    }
}