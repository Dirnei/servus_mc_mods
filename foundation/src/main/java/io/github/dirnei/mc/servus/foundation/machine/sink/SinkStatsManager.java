package io.github.dirnei.mc.servus.foundation.machine.sink;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.dirnei.mc.servus.foundation.ServusFoundation;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the Sink stats from {@code data/servus_foundation/sink/sink.json}. */
public final class SinkStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "sink_stats_manager");

    public static final String DIRECTORY = "sink";
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "sink");

    public static final SinkStatsManager INSTANCE = new SinkStatsManager();

    private volatile SinkStats stats = SinkStats.DEFAULTS;

    private SinkStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement json = files.get(ID);
        SinkStats result = SinkStats.DEFAULTS;
        if (json != null) {
            result = SinkStats.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> ServusFoundation.LOGGER.error("Invalid sink stats {}: {}", ID, error))
                    .orElse(SinkStats.DEFAULTS);
        }
        stats = result;
        ServusFoundation.LOGGER.info("Loaded sink stats: unlimited from chimney height {}, active for {} ticks, "
                        + "{} slots per row, {} mB per tank, {} upgrade slots",
                result.rates().unlimitedHeight(), result.activeTicks(), result.slotsPerRow(), result.tankCapacity(),
                result.upgradeSlots());
    }

    public SinkStats stats() {
        return stats;
    }
}
