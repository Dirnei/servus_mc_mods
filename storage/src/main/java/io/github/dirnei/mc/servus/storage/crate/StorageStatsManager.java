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
    public static final int DEFAULT_RAIL_MAX_SPAN = 3;
    public static final int DEFAULT_TERMINAL_OUTPUT_SLOTS = 9;
    public static final int DEFAULT_PUSH_INTERVAL = 8;
    public static final int DEFAULT_OUTBOUND_CHECK_INTERVAL = 40;
    public static final int DEFAULT_CRAFTING_STATION_TICKS = 40;
    public static final int DEFAULT_STOCK_RULE_INTERVAL = 100;
    public static final int DEFAULT_CRAFTING_STATION_RECIPES = 4;
    public static final double DEFAULT_SHUTTLE_SPEED = 0.25;
    public static final double DEFAULT_LIFT_SPEED = 0.1;
    public static final int DEFAULT_RACK_DISPLAY_COUNT = 4;
    public static final int DEFAULT_RACK_BAYS_PER_SECTION = 3;
    public static final int DEFAULT_INBOUND_BUFFER_SLOTS = 9;
    public static final int DEFAULT_PUT_AWAY_PER_TICK = 1;
    public static final int DEFAULT_CPU_ORDER_SLOTS = 2;
    public static final int DEFAULT_PATTERNS_PER_BLOCK = 9;

    private static final Gson GSON = new GsonBuilder().create();
    private static final ResourceLocation CRATE = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "crate");
    private static final ResourceLocation WAREHOUSE = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "warehouse");
    private static final ResourceLocation RACK = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "rack");
    private static final ResourceLocation CRAFTING_STATION = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "crafting_station");
    private static final ResourceLocation CPU = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "cpu");
    private static final ResourceLocation PATTERN_STORAGE = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "pattern_storage");

    public static final StorageStatsManager INSTANCE = new StorageStatsManager();

    private volatile int crateCapacity = DEFAULT_CRATE_CAPACITY;
    private volatile int sourcesPerTick = DEFAULT_SOURCES_PER_TICK;
    private volatile int rackSlots = DEFAULT_RACK_SLOTS;
    private volatile int garageCapacity = DEFAULT_GARAGE_CAPACITY;
    private volatile int railMaxSpan = DEFAULT_RAIL_MAX_SPAN;
    private volatile int terminalOutputSlots = DEFAULT_TERMINAL_OUTPUT_SLOTS;
    private volatile int pushInterval = DEFAULT_PUSH_INTERVAL;
    private volatile int outboundCheckInterval = DEFAULT_OUTBOUND_CHECK_INTERVAL;
    private volatile int craftingStationTicks = DEFAULT_CRAFTING_STATION_TICKS;
    private volatile int stockRuleInterval = DEFAULT_STOCK_RULE_INTERVAL;
    private volatile int craftingStationRecipes = DEFAULT_CRAFTING_STATION_RECIPES;
    private volatile double shuttleSpeed = DEFAULT_SHUTTLE_SPEED;
    private volatile double liftSpeed = DEFAULT_LIFT_SPEED;
    private volatile int rackDisplayCount = DEFAULT_RACK_DISPLAY_COUNT;
    private volatile int rackBaysPerSection = DEFAULT_RACK_BAYS_PER_SECTION;
    private volatile int inboundBufferSlots = DEFAULT_INBOUND_BUFFER_SLOTS;
    private volatile int putAwayPerTick = DEFAULT_PUT_AWAY_PER_TICK;
    private volatile int cpuOrderSlots = DEFAULT_CPU_ORDER_SLOTS;
    private volatile int patternsPerBlock = DEFAULT_PATTERNS_PER_BLOCK;

    private StorageStatsManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        crateCapacity = intField(files.get(CRATE), "capacity", DEFAULT_CRATE_CAPACITY);
        sourcesPerTick = intField(files.get(WAREHOUSE), "sources_per_tick", DEFAULT_SOURCES_PER_TICK);
        rackSlots = intField(files.get(RACK), "slots", DEFAULT_RACK_SLOTS);
        garageCapacity = intField(files.get(WAREHOUSE), "garage_capacity", DEFAULT_GARAGE_CAPACITY);
        railMaxSpan = intField(files.get(WAREHOUSE), "rail_max_span", DEFAULT_RAIL_MAX_SPAN);
        terminalOutputSlots = intField(files.get(WAREHOUSE), "terminal_output_slots", DEFAULT_TERMINAL_OUTPUT_SLOTS);
        pushInterval = intField(files.get(WAREHOUSE), "push_interval", DEFAULT_PUSH_INTERVAL);
        outboundCheckInterval = intField(files.get(WAREHOUSE), "outbound_check_interval", DEFAULT_OUTBOUND_CHECK_INTERVAL);
        craftingStationTicks = intField(files.get(CRAFTING_STATION), "work_ticks", DEFAULT_CRAFTING_STATION_TICKS);
        stockRuleInterval = intField(files.get(WAREHOUSE), "stock_rule_interval", DEFAULT_STOCK_RULE_INTERVAL);
        craftingStationRecipes = intField(files.get(CRAFTING_STATION), "recipes", DEFAULT_CRAFTING_STATION_RECIPES);
        shuttleSpeed = doubleField(files.get(WAREHOUSE), "shuttle_speed", DEFAULT_SHUTTLE_SPEED);
        liftSpeed = doubleField(files.get(WAREHOUSE), "lift_speed", DEFAULT_LIFT_SPEED);
        rackDisplayCount = intField(files.get(RACK), "display_count", DEFAULT_RACK_DISPLAY_COUNT);
        rackBaysPerSection = intField(files.get(RACK), "bays_per_section", DEFAULT_RACK_BAYS_PER_SECTION);
        inboundBufferSlots = intField(files.get(WAREHOUSE), "inbound_buffer_slots", DEFAULT_INBOUND_BUFFER_SLOTS);
        putAwayPerTick = intField(files.get(WAREHOUSE), "put_away_per_tick", DEFAULT_PUT_AWAY_PER_TICK);
        cpuOrderSlots = intField(files.get(CPU), "order_slots", DEFAULT_CPU_ORDER_SLOTS);
        patternsPerBlock = intField(files.get(PATTERN_STORAGE), "patterns_per_block", DEFAULT_PATTERNS_PER_BLOCK);
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

    /** How many free and hung rails a gap between two holds may span. */
    public int railMaxSpan() {
        return railMaxSpan;
    }

    public int terminalOutputSlots() {
        return terminalOutputSlots;
    }

    public int pushInterval() {
        return pushInterval;
    }

    public int outboundCheckInterval() {
        return outboundCheckInterval;
    }

    public int craftingStationTicks() {
        return craftingStationTicks;
    }

    public int stockRuleInterval() {
        return stockRuleInterval;
    }

    /** Orders one formed Crafting CPU runs at a time. */
    public int cpuOrderSlots() {
        return cpuOrderSlots;
    }

    /** Encoded Patterns one Pattern Storage holds. */
    public int patternsPerBlock() {
        return patternsPerBlock;
    }

    public int craftingStationRecipes() {
        return craftingStationRecipes;
    }

    public double shuttleSpeed() {
        return shuttleSpeed;
    }

    /** Blocks per tick the lift car of an elevator shaft moves. */
    public double liftSpeed() {
        return liftSpeed;
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
