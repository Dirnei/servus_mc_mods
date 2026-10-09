package io.github.dirnei.mc.servus.foundation.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.ServusFoundation;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads the data cable tiers from {@code data/<ns>/data/cable_tiers.json}: an
 * object keyed by the cable item id with {@code items_per_second} and
 * {@code duplex}. Files of every namespace are merged, so other mods can add
 * tiers. An unknown tier counts as the base Data Cable.
 */
public final class CableTiersManager extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "data";
    public static final ResourceLocation BASE_TIER = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "data_cable");

    private static final Gson GSON = new GsonBuilder().create();
    private static final String FILE = "cable_tiers";

    public static final CableTiersManager INSTANCE = new CableTiersManager();

    private volatile Map<ResourceLocation, CableTier> tiers = defaults();

    private CableTiersManager() {
        super(GSON, DIRECTORY);
    }

    /** The tiers the mod ships, used until a datapack is loaded. */
    public static Map<ResourceLocation, CableTier> defaults() {
        Map<ResourceLocation, CableTier> map = new HashMap<>();
        map.put(BASE_TIER, CableTier.BASE);
        map.put(ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "duplex_data_cable"), new CableTier(32, true));
        map.put(ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "fast_data_cable"), new CableTier(128, true));
        return Map.copyOf(map);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, CableTier> read = new HashMap<>(defaults());
        files.forEach((file, element) -> {
            if (!file.getPath().equals(FILE) || !(element instanceof JsonObject json)) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id == null || !(entry.getValue() instanceof JsonObject stats)) {
                    ServusFoundation.LOGGER.warn("Cable tiers: skipping entry {} in {}", entry.getKey(), file);
                    continue;
                }
                CableTier fallback = read.getOrDefault(id, CableTier.BASE);
                read.put(id, new CableTier(
                        stats.has("items_per_second") ? stats.get("items_per_second").getAsDouble() : fallback.itemsPerSecond(),
                        stats.has("duplex") ? stats.get("duplex").getAsBoolean() : fallback.duplex()));
            }
        });
        tiers = Map.copyOf(read);
        ServusFoundation.LOGGER.info("Data cable tiers: {}", tiers);
    }

    /** The stats of a tier; an unknown tier counts as the base Data Cable. */
    public CableTier tier(ResourceLocation id) {
        CableTier tier = id == null ? null : tiers.get(id);
        if (tier != null) {
            return tier;
        }
        return tiers.getOrDefault(BASE_TIER, CableTier.BASE);
    }

    /** For tests: sets one tier as if a datapack had been loaded. */
    public void set(ResourceLocation id, CableTier tier) {
        Map<ResourceLocation, CableTier> copy = new HashMap<>(tiers);
        copy.put(id, tier);
        tiers = Map.copyOf(copy);
    }

    /** For tests: back to the shipped tiers. */
    public void reset() {
        tiers = defaults();
    }
}
