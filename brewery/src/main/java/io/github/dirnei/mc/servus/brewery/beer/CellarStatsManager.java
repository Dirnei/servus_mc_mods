package io.github.dirnei.mc.servus.brewery.beer;

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

/** Loads the vat and Cask balance numbers from {@code data/servus_brewery/cellar/stats.json}. */
public final class CellarStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "cellar_stats_manager");
    private static final Identifier STATS = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "stats");

    /** The fields of the stats file, each optional. */
    public static final Codec<CellarStats.Partial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("vat_capacity").forGetter(CellarStats.Partial::vatCapacity),
            Codec.INT.optionalFieldOf("cask_capacity").forGetter(CellarStats.Partial::caskCapacity)
    ).apply(instance, CellarStats.Partial::new));

    public static final CellarStatsManager INSTANCE = new CellarStatsManager();

    private volatile CellarStats stats = CellarStats.DEFAULTS;

    private CellarStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("cellar"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement json = files.get(STATS);
        stats = json == null ? CellarStats.DEFAULTS : CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusBrewery.LOGGER.error("Invalid cellar stats: {}", error))
                .map(partial -> partial.over(CellarStats.DEFAULTS))
                .orElse(CellarStats.DEFAULTS);
    }

    /** The loaded stats, or the defaults before the first load. */
    public CellarStats stats() {
        return stats;
    }
}
