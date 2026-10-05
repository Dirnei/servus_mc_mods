package io.github.dirnei.mc.servus.storage.crate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.storage.ServusStorage;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads storage block stats from {@code data/<ns>/storage/<name>.json}. */
public final class StorageStatsManager extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "storage";
    public static final int DEFAULT_CRATE_CAPACITY = 4096;
    public static final int DEFAULT_SOURCES_PER_TICK = 4;

    private static final Gson GSON = new GsonBuilder().create();
    private static final ResourceLocation CRATE = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "crate");
    private static final ResourceLocation WAREHOUSE = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "warehouse");

    public static final StorageStatsManager INSTANCE = new StorageStatsManager();

    private volatile int crateCapacity = DEFAULT_CRATE_CAPACITY;
    private volatile int sourcesPerTick = DEFAULT_SOURCES_PER_TICK;

    private StorageStatsManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        crateCapacity = intField(files.get(CRATE), "capacity", DEFAULT_CRATE_CAPACITY);
        sourcesPerTick = intField(files.get(WAREHOUSE), "sources_per_tick", DEFAULT_SOURCES_PER_TICK);
        ServusStorage.LOGGER.info("Crate capacity {}, warehouse polls {} sources per tick", crateCapacity, sourcesPerTick);
    }

    private static int intField(JsonElement element, String field, int fallback) {
        if (element instanceof JsonObject object && object.has(field)) {
            return Math.max(1, object.get(field).getAsInt());
        }
        return fallback;
    }

    public int crateCapacity() {
        return crateCapacity;
    }

    public int sourcesPerTick() {
        return sourcesPerTick;
    }
}
