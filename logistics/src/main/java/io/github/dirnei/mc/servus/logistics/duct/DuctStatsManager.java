package io.github.dirnei.mc.servus.logistics.duct;

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

/** Loads duct stats from {@code data/<ns>/duct/<name>.json}. */
public final class DuctStatsManager extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "duct";

    private static final Gson GSON = new GsonBuilder().create();
    private static final ResourceLocation UTILITY_DUCT = ResourceLocation.fromNamespaceAndPath(ServusLogisticsIds.MOD_ID, "utility_duct");

    public static final DuctStatsManager INSTANCE = new DuctStatsManager();

    private volatile int itemsPerTick = 16;
    private volatile int itemsPerPull = 8;
    private volatile int pullInterval = 8;
    private volatile int energyPerTick = 512;

    private DuctStatsManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement duct = files.get(UTILITY_DUCT);
        itemsPerTick = intField(duct, "items_per_tick", 16);
        itemsPerPull = intField(duct, "items_per_pull", 8);
        pullInterval = intField(duct, "pull_interval", 8);
        energyPerTick = intField(duct, "energy_per_tick", 512);
        ServusLogistics.LOGGER.info("Utility duct: {} items/t, pulls {} every {} t, {} FE/t",
                itemsPerTick, itemsPerPull, pullInterval, energyPerTick);
    }

    private static int intField(JsonElement element, String field, int fallback) {
        if (element instanceof JsonObject object && object.has(field)) {
            return Math.max(1, object.get(field).getAsInt());
        }
        return fallback;
    }

    public int itemsPerTick() {
        return itemsPerTick;
    }

    public int itemsPerPull() {
        return itemsPerPull;
    }

    public int pullInterval() {
        return pullInterval;
    }

    public int energyPerTick() {
        return energyPerTick;
    }
}
