package io.github.dirnei.mc.servus.foundation.drive;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.foundation.ServusFoundation;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads driver stats from {@code data/<ns>/drive/}. */
public final class DriveStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "drive_stats_manager");

    /** The fields of a drive file, each optional. */
    public static final Codec<DriveStats.Partial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("work_per_tick").forGetter(DriveStats.Partial::workPerTick),
            Codec.INT.optionalFieldOf("work_per_turn").forGetter(DriveStats.Partial::workPerTurn),
            Codec.FLOAT.optionalFieldOf("exhaustion_per_turn").forGetter(DriveStats.Partial::exhaustionPerTurn)
    ).apply(instance, DriveStats.Partial::new));

    public static final DriveStatsManager INSTANCE = new DriveStatsManager();

    private volatile Map<Identifier, DriveStats.Partial> loaded = Map.of();

    private DriveStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("drive"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, DriveStats.Partial> result = new HashMap<>();
        files.forEach((id, json) -> CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusFoundation.LOGGER.error("Invalid drive stats {}: {}", id, error))
                .ifPresent(partial -> result.put(id, partial)));
        loaded = Map.copyOf(result);
        ServusFoundation.LOGGER.info("Loaded stats for {} drivers", result.size());
    }

    /** Stats for a driver, with data from the pack merged over the code defaults. */
    public DriveStats get(Identifier driverId, DriveStats defaults) {
        DriveStats.Partial partial = loaded.get(driverId);
        return partial == null ? defaults : partial.over(defaults);
    }
}
