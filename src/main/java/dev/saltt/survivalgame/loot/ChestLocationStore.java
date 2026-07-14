package dev.saltt.survivalgame.loot;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.util.Config;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChestLocationStore {

    private final Config<ChestRegistryData> config;

    public ChestLocationStore(Config<ChestRegistryData> config) {
        this.config = config;
    }

    public Map<String, Vector3i> load() {
        Map<String, Vector3i> result = new LinkedHashMap<>();
        for (ChestEntry entry : config.get().getChests()) {
            result.put(entry.getChestId(), entry.getLocation());
        }
        return result;
    }

    public void addChest(String chestId, Vector3i location) {
        List<ChestEntry> chests = new ArrayList<>(Arrays.asList(config.get().getChests()));
        chests.removeIf(e -> e.getChestId().equals(chestId));
        chests.add(new ChestEntry(chestId, location));
        config.get().setChests(chests.toArray(new ChestEntry[0]));
        config.save();
    }

    public boolean removeChest(String chestId) {
        List<ChestEntry> chests = new ArrayList<>(Arrays.asList(config.get().getChests()));
        boolean removed = chests.removeIf(e -> e.getChestId().equals(chestId));
        if (removed) {
            config.get().setChests(chests.toArray(new ChestEntry[0]));
            config.save();
        }
        return removed;
    }


}

