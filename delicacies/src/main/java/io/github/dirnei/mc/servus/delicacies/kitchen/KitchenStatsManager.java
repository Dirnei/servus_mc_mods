package io.github.dirnei.mc.servus.delicacies.kitchen;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.delicacies.ServusDelicacies;
import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the kitchen's balance numbers from {@code data/servus_delicacies/kitchen/stats.json}. */
public final class KitchenStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "kitchen_stats_manager");
    private static final Identifier STATS = Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "stats");

    /** The fields of the stats file, each optional. */
    public static final Codec<KitchenStats.Partial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("stove_max_length").forGetter(KitchenStats.Partial::stoveMaxLength),
            Codec.INT.optionalFieldOf("pot_capacity").forGetter(KitchenStats.Partial::potCapacity)
    ).apply(instance, KitchenStats.Partial::new));

    public static final KitchenStatsManager INSTANCE = new KitchenStatsManager();

    private volatile KitchenStats stats = KitchenStats.DEFAULTS;

    private KitchenStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("kitchen"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement json = files.get(STATS);
        stats = json == null ? KitchenStats.DEFAULTS : CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusDelicacies.LOGGER.error("Invalid kitchen stats: {}", error))
                .map(partial -> partial.over(KitchenStats.DEFAULTS))
                .orElse(KitchenStats.DEFAULTS);
    }

    /** The loaded stats, or the defaults before the first load. */
    public KitchenStats stats() {
        return stats;
    }

    /** Replaces the loaded stats, for GameTests. */
    public void override(KitchenStats stats) {
        this.stats = stats;
    }
}
