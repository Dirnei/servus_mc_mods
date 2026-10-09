package io.github.dirnei.mc.servus.foundation.upgrade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.ServusFoundation;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads upgrade multipliers from {@code data/<ns>/data/upgrades.json}: an
 * object keyed by item id with {@code speed_multiplier}. Files of every
 * namespace are merged, so other mods add their own upgrades.
 */
public final class UpgradesManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String FILE = "upgrades";

    public static final UpgradesManager INSTANCE = new UpgradesManager();

    private volatile Map<ResourceLocation, Double> multipliers = Map.of();

    private UpgradesManager() {
        super(GSON, "data");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, Double> read = new HashMap<>();
        files.forEach((file, element) -> {
            if (!file.getPath().equals(FILE) || !(element instanceof JsonObject json)) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id == null || !(entry.getValue() instanceof JsonObject stats) || !stats.has("speed_multiplier")) {
                    ServusFoundation.LOGGER.warn("Upgrades: skipping entry {} in {}", entry.getKey(), file);
                    continue;
                }
                read.put(id, stats.get("speed_multiplier").getAsDouble());
            }
        });
        multipliers = Map.copyOf(read);
        ServusFoundation.LOGGER.info("Upgrades: {}", multipliers);
    }

    /** The multiplier a datapack gives the item, null when none does. */
    public Double multiplier(ResourceLocation id) {
        return multipliers.get(id);
    }

    /** For tests: sets a multiplier as if a datapack had been loaded. */
    public void set(ResourceLocation id, double multiplier) {
        Map<ResourceLocation, Double> copy = new HashMap<>(multipliers);
        copy.put(id, multiplier);
        multipliers = Map.copyOf(copy);
    }

    /** For tests: removes a multiplier set with {@link #set}. */
    public void remove(ResourceLocation id) {
        Map<ResourceLocation, Double> copy = new HashMap<>(multipliers);
        copy.remove(id);
        multipliers = Map.copyOf(copy);
    }
}
