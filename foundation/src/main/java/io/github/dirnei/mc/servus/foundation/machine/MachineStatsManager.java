package io.github.dirnei.mc.servus.foundation.machine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.dirnei.mc.servus.foundation.ServusFoundation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads machine stats from datapacks. */
public final class MachineStatsManager extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "machine";

    private static final Gson GSON = new GsonBuilder().create();

    public static final MachineStatsManager INSTANCE = new MachineStatsManager();

    private Map<ResourceLocation, MachineStats.Partial> loaded = Map.of();

    private MachineStatsManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, MachineStats.Partial> result = new HashMap<>();
        files.forEach((id, json) -> MachineStats.Partial.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusFoundation.LOGGER.error("Invalid machine stats {}: {}", id, error))
                .ifPresent(partial -> result.put(id, partial)));
        loaded = Collections.unmodifiableMap(result);
        ServusFoundation.LOGGER.info("Loaded stats for {} machines", result.size());
    }

    /** Stats for a machine, with data from the pack merged over the code defaults. */
    public MachineStats get(ResourceLocation machineId, MachineStats defaults) {
        MachineStats.Partial partial = loaded.get(machineId);
        return partial == null ? defaults : partial.over(defaults);
    }
}
