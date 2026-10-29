package io.github.dirnei.mc.servus.delicacies.aquarium;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.delicacies.ServusDelicacies;
import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the aquarium's balance numbers from {@code data/servus_delicacies/aquarium/aquarium.json}. */
public final class AquariumStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "aquarium_stats_manager");
    private static final Identifier FILE = Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "aquarium");

    public static final AquariumStatsManager INSTANCE = new AquariumStatsManager();

    private volatile AquariumStats stats = AquariumStats.DEFAULTS;

    private AquariumStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("aquarium"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement json = files.get(FILE);
        if (json == null || !json.isJsonObject()) {
            stats = AquariumStats.DEFAULTS;
            return;
        }
        try {
            stats = parse(json.getAsJsonObject()).over(AquariumStats.DEFAULTS);
        } catch (RuntimeException e) {
            ServusDelicacies.LOGGER.error("Invalid aquarium stats: {}", e.getMessage());
            stats = AquariumStats.DEFAULTS;
        }
    }

    /** The fields of the stats file, each optional; more than a record codec builder takes, so read by hand. */
    static AquariumStats.Partial parse(JsonObject json) {
        return new AquariumStats.Partial(integer(json, "cycle_ticks"), integer(json, "water_per_fish"), decimal(json, "breed_rate"),
                decimal(json, "feed_per_fish"), integer(json, "starve_after"), decimal(json, "starve_loss"),
                decimal(json, "dirt_per_fish"), decimal(json, "dirt_per_feed"), decimal(json, "dirt_per_bucket"),
                decimal(json, "dying_below"), decimal(json, "dirty_loss"), integer(json, "filter_rate"),
                integer(json, "fe_per_tank_block"), integer(json, "fe_per_lid"), integer(json, "energy_buffer_ticks"),
                integer(json, "keep_default"), integer(json, "drawn_cap"));
    }

    private static Optional<Integer> integer(JsonObject json, String key) {
        return json.has(key) ? Optional.of(json.get(key).getAsInt()) : Optional.empty();
    }

    private static Optional<Double> decimal(JsonObject json, String key) {
        return json.has(key) ? Optional.of(json.get(key).getAsDouble()) : Optional.empty();
    }

    /** The loaded stats, or the defaults before the first load. */
    public AquariumStats stats() {
        return stats;
    }

    /** Replaces the loaded stats, for GameTests. */
    public void override(AquariumStats stats) {
        this.stats = stats;
    }
}
