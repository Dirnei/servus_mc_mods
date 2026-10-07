package io.github.dirnei.mc.servus.logistics.tank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.logistics.ServusLogistics;
import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads Fluid Tank stats from {@code data/<ns>/tank/<name>.json}. */
public final class TankStatsManager extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "tank";
    public static final int DEFAULT_CAPACITY_PER_BLOCK = 16_000;

    private static final Gson GSON = new GsonBuilder().create();
    private static final ResourceLocation FLUID_TANK = ResourceLocation.fromNamespaceAndPath(ServusLogisticsIds.MOD_ID, "fluid_tank");

    public static final TankStatsManager INSTANCE = new TankStatsManager();

    private volatile int capacityPerBlock = DEFAULT_CAPACITY_PER_BLOCK;

    private TankStatsManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement tank = files.get(FLUID_TANK);
        int capacity = DEFAULT_CAPACITY_PER_BLOCK;
        if (tank instanceof JsonObject object && object.has("capacity_per_block")) {
            capacity = Math.max(1, object.get("capacity_per_block").getAsInt());
        }
        capacityPerBlock = capacity;
        ServusLogistics.LOGGER.info("Fluid tank: {} mB per block", capacityPerBlock);
    }

    /** Capacity of one tank block in mB. */
    public int capacityPerBlock() {
        return capacityPerBlock;
    }
}
