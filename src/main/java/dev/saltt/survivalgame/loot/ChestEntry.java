package dev.saltt.survivalgame.loot;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.joml.Vector3i;

public class ChestEntry {

    private String ChestId;
    private int X;
    private int Y;
    private int Z;

    public ChestEntry() {
    }

    public ChestEntry(String chestId, Vector3i location) {
        this.ChestId = chestId;
        setLocation(location);
    }

    public String getChestId() {
        return ChestId;
    }

    public void setChestId(String chestId) {
        this.ChestId = chestId;
    }

    public Vector3i getLocation() {
        return new Vector3i(X, Y, Z);
    }

    public void setLocation(Vector3i location) {
        this.X = location.x;
        this.Y = location.y;
        this.Z = location.z;
    }

    public static final BuilderCodec<ChestEntry> CODEC = BuilderCodec.builder(ChestEntry.class, ChestEntry::new)
            .append(new KeyedCodec<String>("ChestId", Codec.STRING),
                    (entry, value) -> entry.ChestId = value,
                    (entry) -> entry.ChestId)
            .add()
            .append(new KeyedCodec<Integer>("X", Codec.INTEGER),
                    (entry, value) -> entry.X = value,
                    (entry) -> entry.X)
            .add()
            .append(new KeyedCodec<Integer>("Y", Codec.INTEGER),
                    (entry, value) -> entry.Y = value,
                    (entry) -> entry.Y)
            .add()
            .append(new KeyedCodec<Integer>("Z", Codec.INTEGER),
                    (entry, value) -> entry.Z = value,
                    (entry) -> entry.Z)
            .add()
            .build();
}

