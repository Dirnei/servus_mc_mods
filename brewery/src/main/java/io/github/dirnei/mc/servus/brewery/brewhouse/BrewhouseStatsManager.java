package io.github.dirnei.mc.servus.brewery.brewhouse;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.brewery.ServusBrewery;
import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the Brewhouse's balance numbers from {@code data/servus_brewery/brewhouse/stats.json}. */
public final class BrewhouseStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "brewhouse_stats_manager");
    private static final Identifier STATS = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "stats");

    /** The fields of the stats file, each optional. */
    public static final Codec<BrewhouseStats.Partial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("kettle_capacity").forGetter(BrewhouseStats.Partial::kettleCapacity),
            Codec.INT.optionalFieldOf("wort_capacity").forGetter(BrewhouseStats.Partial::wortCapacity)
    ).apply(instance, BrewhouseStats.Partial::new));

    public static final BrewhouseStatsManager INSTANCE = new BrewhouseStatsManager();

    private volatile BrewhouseStats stats = BrewhouseStats.DEFAULTS;

    private BrewhouseStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("brewhouse"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement json = files.get(STATS);
        stats = json == null ? BrewhouseStats.DEFAULTS : CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusBrewery.LOGGER.error("Invalid Brewhouse stats: {}", error))
                .map(partial -> partial.over(BrewhouseStats.DEFAULTS))
                .orElse(BrewhouseStats.DEFAULTS);
    }

    /** The loaded stats, or the defaults before the first load. */
    public BrewhouseStats stats() {
        return stats;
    }
}
