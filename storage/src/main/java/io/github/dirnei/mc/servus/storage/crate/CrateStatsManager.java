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
public final class CrateStatsManager extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "storage";
    public static final int DEFAULT_CRATE_CAPACITY = 4096;

    private static final Gson GSON = new GsonBuilder().create();
    private static final ResourceLocation CRATE = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "crate");

    public static final CrateStatsManager INSTANCE = new CrateStatsManager();

    private volatile int crateCapacity = DEFAULT_CRATE_CAPACITY;

    private CrateStatsManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement crate = files.get(CRATE);
        int capacity = DEFAULT_CRATE_CAPACITY;
        if (crate instanceof JsonObject object && object.has("capacity")) {
            capacity = Math.max(1, object.get("capacity").getAsInt());
        }
        crateCapacity = capacity;
        ServusStorage.LOGGER.info("Crate capacity {}", capacity);
    }

    public int crateCapacity() {
        return crateCapacity;
    }
}
