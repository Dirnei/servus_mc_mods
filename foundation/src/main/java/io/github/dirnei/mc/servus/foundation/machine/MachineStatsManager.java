package io.github.dirnei.mc.servus.foundation.machine;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.util.ExtraCodecs;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.dirnei.mc.servus.foundation.ServusFoundation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads machine stats from datapacks. */
public final class MachineStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath("servus_foundation", "machine_stats_manager");

    public static final String DIRECTORY = "machine";

    public static final MachineStatsManager INSTANCE = new MachineStatsManager();

    private Map<Identifier, MachineStats.Partial> loaded = Map.of();

    private MachineStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, MachineStats.Partial> result = new HashMap<>();
        files.forEach((id, json) -> MachineStats.Partial.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusFoundation.LOGGER.error("Invalid machine stats {}: {}", id, error))
                .ifPresent(partial -> result.put(id, partial)));
        loaded = Collections.unmodifiableMap(result);
        ServusFoundation.LOGGER.info("Loaded stats for {} machines", result.size());
    }

    /** Stats for a machine, with data from the pack merged over the code defaults. */
    public MachineStats get(Identifier machineId, MachineStats defaults) {
        MachineStats.Partial partial = loaded.get(machineId);
        return partial == null ? defaults : partial.over(defaults);
    }
}
