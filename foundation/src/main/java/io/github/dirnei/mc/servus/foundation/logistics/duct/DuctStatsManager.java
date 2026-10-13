package io.github.dirnei.mc.servus.foundation.logistics.duct;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.ServusLogisticsContent;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads duct stats from {@code data/<ns>/duct/<name>.json}. */
public final class DuctStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "duct_stats_manager");

    public static final String DIRECTORY = "duct";

    private static final Identifier UTILITY_DUCT = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "utility_duct");

    public static final DuctStatsManager INSTANCE = new DuctStatsManager();

    private static final int DEFAULT_ITEMS_PER_TICK = 16;
    private static final int DEFAULT_ITEMS_PER_PULL = 8;
    private static final int DEFAULT_PULL_INTERVAL = 8;
    private static final int DEFAULT_ENERGY_PER_TICK = 512;

    private volatile int itemsPerTick = DEFAULT_ITEMS_PER_TICK;
    private volatile int itemsPerPull = DEFAULT_ITEMS_PER_PULL;
    private volatile int pullInterval = DEFAULT_PULL_INTERVAL;
    private volatile int energyPerTick = DEFAULT_ENERGY_PER_TICK;

    private DuctStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement duct = files.get(UTILITY_DUCT);
        itemsPerTick = intField(duct, "items_per_tick", DEFAULT_ITEMS_PER_TICK);
        itemsPerPull = intField(duct, "items_per_pull", DEFAULT_ITEMS_PER_PULL);
        pullInterval = intField(duct, "pull_interval", DEFAULT_PULL_INTERVAL);
        energyPerTick = intField(duct, "energy_per_tick", DEFAULT_ENERGY_PER_TICK);
        ServusLogisticsContent.LOGGER.info("Utility duct: {} items/t, pulls {} every {} t, {} FE/t",
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
