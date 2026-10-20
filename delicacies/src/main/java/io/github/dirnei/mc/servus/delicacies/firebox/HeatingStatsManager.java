package io.github.dirnei.mc.servus.delicacies.firebox;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.delicacies.ServusDelicacies;
import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

/**
 * Loads the Heating Element numbers of every firebox from {@code data/<ns>/heating/<firebox>.json}; the file id is the
 * firebox id, such as {@code servus_delicacies:tiled_stove} or {@code servus_brewery:brewhouse}.
 */
public final class HeatingStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "heating_stats_manager");
    /** The Tiled Stove's firebox id. */
    public static final Identifier TILED_STOVE = Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "tiled_stove");

    /** The fields of a heating file, each optional. */
    public static final Codec<HeatingStats.Partial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("energy_per_tick").forGetter(HeatingStats.Partial::energyPerTick),
            Codec.INT.optionalFieldOf("energy_buffer").forGetter(HeatingStats.Partial::energyBuffer),
            Codec.INT.optionalFieldOf("heat_up_ticks").forGetter(HeatingStats.Partial::heatUpTicks),
            Codec.INT.optionalFieldOf("cool_down_ticks").forGetter(HeatingStats.Partial::coolDownTicks),
            Codec.FLOAT.optionalFieldOf("hot_at").forGetter(HeatingStats.Partial::hotAt)
    ).apply(instance, HeatingStats.Partial::new));

    public static final HeatingStatsManager INSTANCE = new HeatingStatsManager();

    private volatile Map<Identifier, HeatingStats.Partial> loaded = Map.of();
    private final Map<Identifier, HeatingStats> overrides = new ConcurrentHashMap<>();

    private HeatingStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("heating"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, HeatingStats.Partial> result = new HashMap<>();
        files.forEach((id, json) -> CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusDelicacies.LOGGER.error("Invalid heating stats {}: {}", id, error))
                .ifPresent(partial -> result.put(id, partial)));
        loaded = Map.copyOf(result);
    }

    /** The numbers of a firebox, with the pack's file merged over the firebox's code defaults. */
    public HeatingStats get(Identifier firebox, HeatingStats defaults) {
        HeatingStats override = overrides.get(firebox);
        if (override != null) {
            return override;
        }
        HeatingStats.Partial partial = loaded.get(firebox);
        return partial == null ? defaults : partial.over(defaults);
    }

    /** Replaces a firebox's numbers until cleared with null, for GameTests. */
    public void override(Identifier firebox, @Nullable HeatingStats stats) {
        if (stats == null) {
            overrides.remove(firebox);
        } else {
            overrides.put(firebox, stats);
        }
    }
}
