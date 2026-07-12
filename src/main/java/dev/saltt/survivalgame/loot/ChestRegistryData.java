package dev.saltt.survivalgame.loot;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

public class ChestRegistryData {

    private ChestEntry[] Chests = new ChestEntry[0];

    public ChestRegistryData() {
    }

    public ChestEntry[] getChests() {
        return Chests;
    }

    public void setChests(ChestEntry[] chests) {
        this.Chests = chests;
    }

    public static final BuilderCodec<ChestRegistryData> CODEC =
            BuilderCodec.builder(ChestRegistryData.class, ChestRegistryData::new)
                    .append(new KeyedCodec<ChestEntry[]>("Chests", new ArrayCodec<>(ChestEntry.CODEC, ChestEntry[]::new)),
                            (data, value) -> data.Chests = value,
                            (data) -> data.Chests)
                    .add()
                    .build();
}
