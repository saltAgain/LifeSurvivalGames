package dev.saltt.survivalgame.loot;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

public class ChestLootConfig {

    public static final BuilderCodec<ChestLootConfig> CODEC =
            BuilderCodec.builder(ChestLootConfig.class, ChestLootConfig::new)
                    .append(new KeyedCodec<>("Chests",
                                    ArrayCodec.ofBuilderCodec(ChestDef.CODEC, ChestDef[]::new)),
                            (config, value) -> config.chests = value,
                            (config) -> config.chests).add()
                    .build();

    private ChestDef[] chests = new ChestDef[0];

    public ChestLootConfig() {
    }

    public ChestDef[] getChests() {
        return chests;
    }

    public void setChests(ChestDef[] chests) {
        this.chests = chests;
    }

    public static class ChestDef {

        public static final BuilderCodec<ChestDef> CODEC =
                BuilderCodec.builder(ChestDef.class, ChestDef::new)
                        .append(new KeyedCodec<>("ChestItemId", Codec.STRING),
                                (def, value) -> def.chestItemId = value,
                                (def) -> def.chestItemId).add()
                        .append(new KeyedCodec<>("InitialSpawnTable", Codec.STRING),
                                (def, value) -> def.initialSpawnTable = value,
                                (def) -> def.initialSpawnTable).add()
                        .append(new KeyedCodec<>("RespawnTable", Codec.STRING),
                                (def, value) -> def.respawnTable = value,
                                (def) -> def.respawnTable).add()
                        .build();

        private String chestItemId = "";
        private String initialSpawnTable = "";
        private String respawnTable = "";

        public ChestDef() {
        }

        public String getChestItemId() {
            return chestItemId;
        }

        public void setChestItemId(String chestItemId) {
            this.chestItemId = chestItemId;
        }

        public String getInitialSpawnTable() {
            return initialSpawnTable;
        }

        public void setInitialSpawnTable(String initialSpawnTable) {
            this.initialSpawnTable = initialSpawnTable;
        }

        public String getRespawnTable() {
            return respawnTable;
        }

        public void setRespawnTable(String respawnTable) {
            this.respawnTable = respawnTable;
        }
    }
}