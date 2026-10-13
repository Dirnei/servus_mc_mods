package io.github.dirnei.mc.servus.foundation.data;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.util.ExtraCodecs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.ServusFoundation;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads the data cable tiers from {@code data/<ns>/data/cable_tiers.json}: an
 * object keyed by the cable item id with {@code items_per_second} and
 * {@code duplex}. Files of every namespace are merged, so other mods can add
 * tiers. An unknown tier counts as the base Data Cable.
 */
public final class CableTiersManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "cable_tiers_manager");

    public static final String DIRECTORY = "data";
    public static final Identifier BASE_TIER = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "data_cable");

    private static final String FILE = "cable_tiers";

    public static final CableTiersManager INSTANCE = new CableTiersManager();

    private volatile Map<Identifier, CableTier> tiers = defaults();

    private CableTiersManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    /** The tiers the mod ships, used until a datapack is loaded. */
    private static Map<Identifier, CableTier> defaults() {
        Map<Identifier, CableTier> map = new HashMap<>();
        map.put(BASE_TIER, CableTier.BASE);
        map.put(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "duplex_data_cable"), new CableTier(32, true));
        map.put(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "fast_data_cable"), new CableTier(128, true));
        return Map.copyOf(map);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, CableTier> read = new HashMap<>(defaults());
        files.forEach((file, element) -> {
            if (!file.getPath().equals(FILE) || !(element instanceof JsonObject json)) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                Identifier id = Identifier.tryParse(entry.getKey());
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
    public CableTier tier(Identifier id) {
        CableTier tier = id == null ? null : tiers.get(id);
        if (tier != null) {
            return tier;
        }
        return tiers.getOrDefault(BASE_TIER, CableTier.BASE);
    }
}
