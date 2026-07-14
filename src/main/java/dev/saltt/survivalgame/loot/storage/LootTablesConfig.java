package dev.saltt.survivalgame.loot.storage;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

public class LootTablesConfig {

    public static final BuilderCodec<LootTablesConfig> CODEC =
            BuilderCodec.builder(LootTablesConfig.class, LootTablesConfig::new)
                    .append(new KeyedCodec<>("Tables",
                                    ArrayCodec.ofBuilderCodec(TableDef.CODEC, TableDef[]::new)),
                            (config, value) -> config.tables = value,
                            (config) -> config.tables).add()
                    .build();

    private TableDef[] tables = new TableDef[0];

    public LootTablesConfig() {
    }

    public TableDef[] getTables() {
        return tables;
    }

    public void setTables(TableDef[] tables) {
        this.tables = tables;
    }

    public static class TableDef {

        public static final BuilderCodec<TableDef> CODEC =
                BuilderCodec.builder(TableDef.class, TableDef::new)
                        .append(new KeyedCodec<>("TableId", Codec.STRING),
                                (def, value) -> def.tableId = value,
                                (def) -> def.tableId).add()
                        .append(new KeyedCodec<>("Entries",
                                        ArrayCodec.ofBuilderCodec(EntryDef.CODEC, EntryDef[]::new)),
                                (def, value) -> def.entries = value,
                                (def) -> def.entries).add()
                        .build();

        private String tableId = "";
        private EntryDef[] entries = new EntryDef[0];

        public TableDef() {
        }

        public String getTableId() {
            return tableId;
        }

        public void setTableId(String tableId) {
            this.tableId = tableId;
        }

        public EntryDef[] getEntries() {
            return entries;
        }

        public void setEntries(EntryDef[] entries) {
            this.entries = entries;
        }
    }

    public static class EntryDef {

        public static final BuilderCodec<EntryDef> CODEC =
                BuilderCodec.builder(EntryDef.class, EntryDef::new)
                        .append(new KeyedCodec<>("ItemId", Codec.STRING),
                                (def, value) -> def.itemId = value,
                                (def) -> def.itemId).add()
                        .append(new KeyedCodec<>("Quantities", Codec.INT_ARRAY),
                                (def, value) -> def.quantities = value,
                                (def) -> def.quantities).add()
                        .build();

        private String itemId = "";
        private int[] quantities = new int[]{1};

        public EntryDef() {
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public int[] getQuantities() {
            return quantities;
        }

        public void setQuantities(int[] quantities) {
            this.quantities = quantities;
        }
    }
}