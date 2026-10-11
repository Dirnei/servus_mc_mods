package io.github.dirnei.mc.servus.foundation.logistics.tank;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.util.ExtraCodecs;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.logistics.ServusLogisticsContent;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads Fluid Tank stats from {@code data/<ns>/tank/<name>.json}. */
public final class TankStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath("servus_foundation", "tank_stats_manager");

    public static final String DIRECTORY = "tank";
    public static final int DEFAULT_CAPACITY_PER_BLOCK = 16_000;

    private static final Gson GSON = new GsonBuilder().create();
    private static final Identifier FLUID_TANK = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "fluid_tank");

    public static final TankStatsManager INSTANCE = new TankStatsManager();

    private volatile int capacityPerBlock = DEFAULT_CAPACITY_PER_BLOCK;

    private TankStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement tank = files.get(FLUID_TANK);
        int capacity = DEFAULT_CAPACITY_PER_BLOCK;
        if (tank instanceof JsonObject object && object.has("capacity_per_block")) {
            capacity = Math.max(1, object.get("capacity_per_block").getAsInt());
        }
        capacityPerBlock = capacity;
        ServusLogisticsContent.LOGGER.info("Fluid tank: {} mB per block", capacityPerBlock);
    }

    /** Capacity of one tank block in mB. */
    public int capacityPerBlock() {
        return capacityPerBlock;
    }
}
