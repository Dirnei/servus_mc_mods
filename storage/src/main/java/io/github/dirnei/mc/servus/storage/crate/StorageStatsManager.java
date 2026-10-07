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
    public static final int DEFAULT_RACK_SLOTS = 27;
    public static final int DEFAULT_GARAGE_CAPACITY = 4;
    public static final int DEFAULT_TERMINAL_OUTPUT_SLOTS = 9;
    public static final double DEFAULT_SHUTTLE_SPEED = 0.25;
    public static final int DEFAULT_RACK_DISPLAY_COUNT = 4;
    public static final int DEFAULT_RACK_BAYS_PER_SECTION = 3;
    public static final int DEFAULT_INBOUND_BUFFER_SLOTS = 9;
    public static final int DEFAULT_PUT_AWAY_PER_TICK = 1;

    private static final Gson GSON = new GsonBuilder().create();
    private static final ResourceLocation CRATE = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "crate");
    private static final ResourceLocation WAREHOUSE = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "warehouse");
    private static final ResourceLocation RACK = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "rack");

    public static final StorageStatsManager INSTANCE = new StorageStatsManager();

    private volatile int crateCapacity = DEFAULT_CRATE_CAPACITY;
    private volatile int sourcesPerTick = DEFAULT_SOURCES_PER_TICK;
    private volatile int rackSlots = DEFAULT_RACK_SLOTS;
    private volatile int garageCapacity = DEFAULT_GARAGE_CAPACITY;
    private volatile int terminalOutputSlots = DEFAULT_TERMINAL_OUTPUT_SLOTS;
    private volatile double shuttleSpeed = DEFAULT_SHUTTLE_SPEED;
    private volatile int rackDisplayCount = DEFAULT_RACK_DISPLAY_COUNT;
    private volatile int rackBaysPerSection = DEFAULT_RACK_BAYS_PER_SECTION;
    private volatile int inboundBufferSlots = DEFAULT_INBOUND_BUFFER_SLOTS;
    private volatile int putAwayPerTick = DEFAULT_PUT_AWAY_PER_TICK;

    private StorageStatsManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        crateCapacity = intField(files.get(CRATE), "capacity", DEFAULT_CRATE_CAPACITY);
        sourcesPerTick = intField(files.get(WAREHOUSE), "sources_per_tick", DEFAULT_SOURCES_PER_TICK);
        rackSlots = intField(files.get(RACK), "slots", DEFAULT_RACK_SLOTS);
        garageCapacity = intField(files.get(WAREHOUSE), "garage_capacity", DEFAULT_GARAGE_CAPACITY);
        terminalOutputSlots = intField(files.get(WAREHOUSE), "terminal_output_slots", DEFAULT_TERMINAL_OUTPUT_SLOTS);
        shuttleSpeed = doubleField(files.get(WAREHOUSE), "shuttle_speed", DEFAULT_SHUTTLE_SPEED);
        rackDisplayCount = intField(files.get(RACK), "display_count", DEFAULT_RACK_DISPLAY_COUNT);
        rackBaysPerSection = intField(files.get(RACK), "bays_per_section", DEFAULT_RACK_BAYS_PER_SECTION);
        inboundBufferSlots = intField(files.get(WAREHOUSE), "inbound_buffer_slots", DEFAULT_INBOUND_BUFFER_SLOTS);
        putAwayPerTick = intField(files.get(WAREHOUSE), "put_away_per_tick", DEFAULT_PUT_AWAY_PER_TICK);
        ServusStorage.LOGGER.info("Crate capacity {}, warehouse polls {} sources per tick", crateCapacity, sourcesPerTick);
    }

    private static double doubleField(JsonElement element, String field, double fallback) {
        if (element instanceof JsonObject object && object.has(field)) {
            return Math.max(0.01, object.get(field).getAsDouble());
        }
        return fallback;
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

    public int garageCapacity() {
        return garageCapacity;
    }

    public int terminalOutputSlots() {
        return terminalOutputSlots;
    }

    public double shuttleSpeed() {
        return shuttleSpeed;
    }

    public int rackSlots() {
        return rackSlots;
    }

    public int rackDisplayCount() {
        return rackDisplayCount;
    }

    public int rackBaysPerSection() {
        return rackBaysPerSection;
    }

    public int inboundBufferSlots() {
        return inboundBufferSlots;
    }

    public int putAwayPerTick() {
        return putAwayPerTick;
    }
}
