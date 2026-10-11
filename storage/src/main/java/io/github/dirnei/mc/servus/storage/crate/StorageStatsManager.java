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
    public static final int DEFAULT_CARGO_HOLD_STACKS = 4;
    public static final int DEFAULT_RAIL_MAX_SPAN = 3;
    public static final int DEFAULT_TERMINAL_OUTPUT_SLOTS = 9;
    public static final int DEFAULT_PUSH_INTERVAL = 8;
    public static final int DEFAULT_OUTBOUND_CHECK_INTERVAL = 40;
    public static final int DEFAULT_CRAFTING_STATION_TICKS = 40;
    public static final int DEFAULT_STOCK_RULE_INTERVAL = 100;
    public static final double DEFAULT_SHUTTLE_SPEED = 0.25;
    public static final double DEFAULT_LIFT_SPEED = 0.1;
    public static final int DEFAULT_RACK_DISPLAY_COUNT = 4;
    public static final int DEFAULT_RACK_BAYS_PER_SECTION = 3;
    public static final int DEFAULT_INBOUND_BUFFER_SLOTS = 9;
    public static final int DEFAULT_PUT_AWAY_PER_TICK = 1;
    public static final int DEFAULT_CPU_ORDER_SLOTS = 2;
    public static final int DEFAULT_HANDOFF_WAIT_TICKS = 100;
    public static final int DEFAULT_PATTERNS_PER_BLOCK = 9;
    public static final double DEFAULT_PACKING_ITEMS_PER_SECOND = 8.0;
    public static final int DEFAULT_PACKING_UPGRADE_SLOTS = 2;
    public static final int DEFAULT_FIELD_CAPACITY = 8000;
    public static final int DEFAULT_FIELD_WATER_PER_RAIL = 1;
    public static final int DEFAULT_FIELD_WATER_INTERVAL = 100;
    public static final int DEFAULT_FIELD_MAX_RAILS = 128;
    public static final double DEFAULT_FIELD_HARVEST_SHARE = 0.5;
    public static final int DEFAULT_FIELD_WORK_TICKS = 10;
    public static final int DEFAULT_HARVESTER_STACKS = 2;
    public static final int DEFAULT_FIELD_CHECK_INTERVAL = 40;

    private static final Gson GSON = new GsonBuilder().create();
    private static final ResourceLocation CRATE = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "crate");
    private static final ResourceLocation WAREHOUSE = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "warehouse");
    private static final ResourceLocation RACK = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "rack");
    private static final ResourceLocation CRAFTING_STATION = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "crafting_station");
    private static final ResourceLocation CPU = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "cpu");
    private static final ResourceLocation PACKING_STATION = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "packing_station");
    private static final ResourceLocation PATTERN_STORAGE = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "pattern_storage");
    private static final ResourceLocation FIELD = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "field");

    public static final StorageStatsManager INSTANCE = new StorageStatsManager();

    private volatile int crateCapacity = DEFAULT_CRATE_CAPACITY;
    private volatile int sourcesPerTick = DEFAULT_SOURCES_PER_TICK;
    private volatile int rackSlots = DEFAULT_RACK_SLOTS;
    private volatile int garageCapacity = DEFAULT_GARAGE_CAPACITY;
    private volatile int cargoHoldStacks = DEFAULT_CARGO_HOLD_STACKS;
    private volatile int railMaxSpan = DEFAULT_RAIL_MAX_SPAN;
    private volatile int terminalOutputSlots = DEFAULT_TERMINAL_OUTPUT_SLOTS;
    private volatile int pushInterval = DEFAULT_PUSH_INTERVAL;
    private volatile int outboundCheckInterval = DEFAULT_OUTBOUND_CHECK_INTERVAL;
    private volatile int craftingStationTicks = DEFAULT_CRAFTING_STATION_TICKS;
    private volatile int stockRuleInterval = DEFAULT_STOCK_RULE_INTERVAL;
    private volatile double shuttleSpeed = DEFAULT_SHUTTLE_SPEED;
    private volatile double liftSpeed = DEFAULT_LIFT_SPEED;
    private volatile int rackDisplayCount = DEFAULT_RACK_DISPLAY_COUNT;
    private volatile int rackBaysPerSection = DEFAULT_RACK_BAYS_PER_SECTION;
    private volatile int inboundBufferSlots = DEFAULT_INBOUND_BUFFER_SLOTS;
    private volatile int putAwayPerTick = DEFAULT_PUT_AWAY_PER_TICK;
    private volatile int cpuOrderSlots = DEFAULT_CPU_ORDER_SLOTS;
    private volatile int handoffWaitTicks = DEFAULT_HANDOFF_WAIT_TICKS;
    private volatile int patternsPerBlock = DEFAULT_PATTERNS_PER_BLOCK;
    private volatile double packingItemsPerSecond = DEFAULT_PACKING_ITEMS_PER_SECOND;
    private volatile int packingUpgradeSlots = DEFAULT_PACKING_UPGRADE_SLOTS;
    private volatile int fieldCapacity = DEFAULT_FIELD_CAPACITY;
    private volatile int fieldWaterPerRail = DEFAULT_FIELD_WATER_PER_RAIL;
    private volatile int fieldWaterInterval = DEFAULT_FIELD_WATER_INTERVAL;
    private volatile int fieldMaxRails = DEFAULT_FIELD_MAX_RAILS;
    private volatile double fieldHarvestShare = DEFAULT_FIELD_HARVEST_SHARE;
    private volatile int fieldWorkTicks = DEFAULT_FIELD_WORK_TICKS;
    private volatile int harvesterStacks = DEFAULT_HARVESTER_STACKS;
    private volatile int fieldCheckInterval = DEFAULT_FIELD_CHECK_INTERVAL;

    private StorageStatsManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        crateCapacity = intField(files.get(CRATE), "capacity", DEFAULT_CRATE_CAPACITY);
        sourcesPerTick = intField(files.get(WAREHOUSE), "sources_per_tick", DEFAULT_SOURCES_PER_TICK);
        rackSlots = intField(files.get(RACK), "slots", DEFAULT_RACK_SLOTS);
        garageCapacity = intField(files.get(WAREHOUSE), "garage_capacity", DEFAULT_GARAGE_CAPACITY);
        cargoHoldStacks = intField(files.get(WAREHOUSE), "cargo_hold_stacks", DEFAULT_CARGO_HOLD_STACKS);
        railMaxSpan = intField(files.get(WAREHOUSE), "rail_max_span", DEFAULT_RAIL_MAX_SPAN);
        terminalOutputSlots = intField(files.get(WAREHOUSE), "terminal_output_slots", DEFAULT_TERMINAL_OUTPUT_SLOTS);
        pushInterval = intField(files.get(WAREHOUSE), "push_interval", DEFAULT_PUSH_INTERVAL);
        outboundCheckInterval = intField(files.get(WAREHOUSE), "outbound_check_interval", DEFAULT_OUTBOUND_CHECK_INTERVAL);
        craftingStationTicks = intField(files.get(CRAFTING_STATION), "work_ticks", DEFAULT_CRAFTING_STATION_TICKS);
        stockRuleInterval = intField(files.get(WAREHOUSE), "stock_rule_interval", DEFAULT_STOCK_RULE_INTERVAL);
        shuttleSpeed = doubleField(files.get(WAREHOUSE), "shuttle_speed", DEFAULT_SHUTTLE_SPEED);
        liftSpeed = doubleField(files.get(WAREHOUSE), "lift_speed", DEFAULT_LIFT_SPEED);
        rackDisplayCount = intField(files.get(RACK), "display_count", DEFAULT_RACK_DISPLAY_COUNT);
        rackBaysPerSection = intField(files.get(RACK), "bays_per_section", DEFAULT_RACK_BAYS_PER_SECTION);
        inboundBufferSlots = intField(files.get(WAREHOUSE), "inbound_buffer_slots", DEFAULT_INBOUND_BUFFER_SLOTS);
        putAwayPerTick = intField(files.get(WAREHOUSE), "put_away_per_tick", DEFAULT_PUT_AWAY_PER_TICK);
        cpuOrderSlots = intField(files.get(CPU), "order_slots", DEFAULT_CPU_ORDER_SLOTS);
        handoffWaitTicks = intField(files.get(CPU), "handoff_wait_ticks", DEFAULT_HANDOFF_WAIT_TICKS);
        patternsPerBlock = intField(files.get(PATTERN_STORAGE), "patterns_per_block", DEFAULT_PATTERNS_PER_BLOCK);
        packingItemsPerSecond = doubleField(files.get(PACKING_STATION), "items_per_second", DEFAULT_PACKING_ITEMS_PER_SECOND);
        packingUpgradeSlots = intField(files.get(PACKING_STATION), "upgrade_slots", DEFAULT_PACKING_UPGRADE_SLOTS);
        fieldCapacity = intField(files.get(FIELD), "capacity", DEFAULT_FIELD_CAPACITY);
        fieldWaterPerRail = intField(files.get(FIELD), "water_per_rail", DEFAULT_FIELD_WATER_PER_RAIL);
        fieldWaterInterval = intField(files.get(FIELD), "water_interval", DEFAULT_FIELD_WATER_INTERVAL);
        fieldMaxRails = intField(files.get(FIELD), "max_rails", DEFAULT_FIELD_MAX_RAILS);
        fieldHarvestShare = Math.min(1.0, doubleField(files.get(FIELD), "harvest_share", DEFAULT_FIELD_HARVEST_SHARE));
        fieldWorkTicks = intField(files.get(FIELD), "work_ticks", DEFAULT_FIELD_WORK_TICKS);
        harvesterStacks = intField(files.get(FIELD), "harvester_stacks", DEFAULT_HARVESTER_STACKS);
        fieldCheckInterval = intField(files.get(FIELD), "field_check_interval", DEFAULT_FIELD_CHECK_INTERVAL);
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

    /** How many stacks a shuttle with a Cargo Hold carries per trip. */
    public int cargoHoldStacks() {
        return cargoHoldStacks;
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
    /** How long a step's outputs wait at their producer for a busy later step before they go to storage. */
    public int handoffWaitTicks() {
        return handoffWaitTicks;
    }

    public int cpuOrderSlots() {
        return cpuOrderSlots;
    }

    /** Encoded Patterns one Pattern Storage holds. */
    public int patternsPerBlock() {
        return patternsPerBlock;
    }

    /** Items a Packing Station converts per second in each direction. */
    public double packingItemsPerSecond() {
        return packingItemsPerSecond;
    }

    /** Upgrade slots of a Packing Station placed from now on. */
    public int packingUpgradeSlots() {
        return packingUpgradeSlots;
    }

    /** For tests: sets the Packing Station rate as if a datapack had been loaded. */
    public void setPackingItemsPerSecond(double itemsPerSecond) {
        packingItemsPerSecond = itemsPerSecond;
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

    /** mB of water a Field Head buffers. */
    public int fieldCapacity() {
        return fieldCapacity;
    }

    /** mB of water a field uses per farm rail every {@link #fieldWaterInterval()} ticks. */
    public int fieldWaterPerRail() {
        return fieldWaterPerRail;
    }

    /** Ticks between two water uses of a hydrated field. */
    public int fieldWaterInterval() {
        return fieldWaterInterval;
    }

    /** The most farm rails one field may have. */
    public int fieldMaxRails() {
        return fieldMaxRails;
    }

    /** Ripe share of the planted plots at which a field signals harvest due. */
    public double fieldHarvestShare() {
        return fieldHarvestShare;
    }

    /** For tests: sets the water interval as if a datapack had been loaded. */
    public void setFieldWaterInterval(int ticks) {
        fieldWaterInterval = ticks;
    }

    /** Ticks a harvester spends on one plot it works. */
    public int fieldWorkTicks() {
        return fieldWorkTicks;
    }

    /** For tests: sets the work ticks per plot as if a datapack had been loaded. */
    public void setFieldWorkTicks(int ticks) {
        fieldWorkTicks = ticks;
    }

    /** Stacks a harvester carries on a field run at least, so a crop and its seeds fit without a Cargo Hold. */
    public int harvesterStacks() {
        return harvesterStacks;
    }

    /** Ticks between two looks of a warehouse at the Field Heads of its fields. */
    public int fieldCheckInterval() {
        return fieldCheckInterval;
    }

    /** For tests: sets the field check interval as if a datapack had been loaded. */
    public void setFieldCheckInterval(int ticks) {
        fieldCheckInterval = ticks;
    }
}
