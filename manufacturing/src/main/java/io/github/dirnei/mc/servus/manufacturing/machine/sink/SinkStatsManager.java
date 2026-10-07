package io.github.dirnei.mc.servus.manufacturing.machine.sink;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturing;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the Sink stats from {@code data/servus_manufacturing/sink/sink.json}. */
public final class SinkStatsManager extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "sink";
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "sink");

    private static final Gson GSON = new GsonBuilder().create();

    public static final SinkStatsManager INSTANCE = new SinkStatsManager();

    private volatile SinkStats stats = SinkStats.DEFAULTS;

    private SinkStatsManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement json = files.get(ID);
        SinkStats result = SinkStats.DEFAULTS;
        if (json != null) {
            result = SinkStats.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> ServusManufacturing.LOGGER.error("Invalid sink stats {}: {}", ID, error))
                    .orElse(SinkStats.DEFAULTS);
        }
        stats = result;
        ServusManufacturing.LOGGER.info("Loaded sink stats: unlimited from chimney height {}, active for {} ticks",
                result.rates().unlimitedHeight(), result.activeTicks());
    }

    public SinkStats stats() {
        return stats;
    }
}
