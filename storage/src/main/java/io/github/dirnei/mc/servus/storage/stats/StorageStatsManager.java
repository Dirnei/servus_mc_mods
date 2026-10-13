package io.github.dirnei.mc.servus.storage.stats;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.util.ExtraCodecs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.storage.ServusStorage;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleTiers;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads storage block stats from {@code data/<ns>/storage/<name>.json}. */
public final class StorageStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath("servus_storage", "storage_stats_manager");

    public static final String DIRECTORY = "storage";
    public static final int DEFAULT_SOURCES_PER_TICK = 4;
    public static final int DEFAULT_RACK_SLOTS = 27;
    public static final int DEFAULT_GARAGE_CAPACITY = 4;
    public static final int DEFAULT_RAIL_MAX_SPAN = 3;
    public static final int DEFAULT_TERMINAL_OUTPUT_SLOTS = 9;
    public static final int DEFAULT_PUSH_INTERVAL = 8;
    public static final int DEFAULT_OUTBOUND_CHECK_INTERVAL = 40;
    public static final int DEFAULT_CRAFTING_STATION_TICKS = 40;
    public static final int DEFAULT_STOCK_RULE_INTERVAL = 100;
    public static final double DEFAULT_LIFT_SPEED = 0.1;
    public static final int DEFAULT_LIFT_HANDOVER_TICKS = 10;
    public static final double DEFAULT_LIFT_ACCEL = 0.005;
    public static final int DEFAULT_LIFT_UPGRADE_SLOTS = 4;
    public static final double DEFAULT_LIFT_SPEED_PER_UPGRADE = 0.05;
    public static final double DEFAULT_LIFT_ACCEL_PER_UPGRADE = 0.0025;
    public static final int DEFAULT_RACK_DISPLAY_COUNT = 4;
    public static final int DEFAULT_ANCHOR_UPGRADE_SLOTS = 4;
    public static final int DEFAULT_CHUNKS_PER_UPGRADE = 9;
    public static final int DEFAULT_WAKE_GRACE_SECONDS = 30;
    public static final int DEFAULT_CONTROLLER_ENERGY_CAPACITY = 100000;
    public static final double DEFAULT_WIRELESS_FE_PER_ITEM = 4.0;
    public static final double DEFAULT_WIRELESS_FE_PER_ITEM_PER_BLOCK = 0.02;
    public static final double DEFAULT_SHUTTLE_FE_PER_BLOCK = 20;
    public static final double DEFAULT_WAKE_FE_PER_CHUNK_PER_TICK = 2;
    public static final double DEFAULT_CRAFTING_STATION_FE_PER_TICK = 5;
    public static final int DEFAULT_SITE_SNAPSHOT_INTERVAL = 100;
    public static final int DEFAULT_RACK_BAYS_PER_SECTION = 3;
    public static final int DEFAULT_INBOUND_BUFFER_SLOTS = 9;
    public static final int DEFAULT_PUT_AWAY_PER_TICK = 1;
    public static final int DEFAULT_CPU_ORDERS_PER_CPU_BLOCK = 1;
    public static final int DEFAULT_CPU_STACKS_PER_BUFFER_BLOCK = 1;
    public static final int DEFAULT_CPU_MAX_BLOCKS = 512;
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
    public static final int DEFAULT_FIELD_CHECK_INTERVAL = 40;
    public static final int DEFAULT_FIELD_RADIUS = 2;
    public static final int DEFAULT_FIELD_MAX_RADIUS = 4;
    public static final int DEFAULT_FIELD_CORROSION_WATER = 64000;
    public static final int DEFAULT_FIELD_ANTI_CORROSION_LEVEL = 8;
    public static final int DEFAULT_FERTILIZE_INTERVAL = 600;
    public static final int DEFAULT_DOSES_PER_PLOT = 2;
    public static final int DEFAULT_FERTILIZER_RESERVE = 64;
    public static final double DEFAULT_FERTILIZE_SHARE = 0.5;

    private static final Gson GSON = new GsonBuilder().create();
    private static final Identifier WAREHOUSE = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "warehouse");
    private static final Identifier RACK = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "rack");
    private static final Identifier CRAFTING_STATION = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "crafting_station");
    private static final Identifier CPU = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "cpu");
    private static final Identifier PACKING_STATION = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "packing_station");
    private static final Identifier PATTERN_STORAGE = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "pattern_storage");
    private static final Identifier SHUTTLE = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "shuttle");
    private static final Identifier FIELD = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "field");
    private static final Identifier POWER = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "power");

    public static final StorageStatsManager INSTANCE = new StorageStatsManager();

    private volatile int sourcesPerTick = DEFAULT_SOURCES_PER_TICK;
    private volatile int rackSlots = DEFAULT_RACK_SLOTS;
    private volatile int garageCapacity = DEFAULT_GARAGE_CAPACITY;
    private volatile ShuttleTiers shuttleTiers = ShuttleTiers.DEFAULT;
    private volatile int railMaxSpan = DEFAULT_RAIL_MAX_SPAN;
    private volatile int terminalOutputSlots = DEFAULT_TERMINAL_OUTPUT_SLOTS;
    private volatile int pushInterval = DEFAULT_PUSH_INTERVAL;
    private volatile int outboundCheckInterval = DEFAULT_OUTBOUND_CHECK_INTERVAL;
    private volatile int craftingStationTicks = DEFAULT_CRAFTING_STATION_TICKS;
    private volatile int stockRuleInterval = DEFAULT_STOCK_RULE_INTERVAL;
    private volatile double liftSpeed = DEFAULT_LIFT_SPEED;
    private volatile int liftHandoverTicks = DEFAULT_LIFT_HANDOVER_TICKS;
    private volatile double liftAccel = DEFAULT_LIFT_ACCEL;
    private volatile int liftUpgradeSlots = DEFAULT_LIFT_UPGRADE_SLOTS;
    private volatile double liftSpeedPerUpgrade = DEFAULT_LIFT_SPEED_PER_UPGRADE;
    private volatile double liftAccelPerUpgrade = DEFAULT_LIFT_ACCEL_PER_UPGRADE;
    private volatile int rackDisplayCount = DEFAULT_RACK_DISPLAY_COUNT;
    private volatile int anchorUpgradeSlots = DEFAULT_ANCHOR_UPGRADE_SLOTS;
    private volatile int chunksPerUpgrade = DEFAULT_CHUNKS_PER_UPGRADE;
    private volatile int wakeGraceTicks = DEFAULT_WAKE_GRACE_SECONDS * 20;
    private volatile int siteSnapshotInterval = DEFAULT_SITE_SNAPSHOT_INTERVAL;
    private volatile int controllerEnergyCapacity = DEFAULT_CONTROLLER_ENERGY_CAPACITY;
    private volatile double wirelessFePerItem = DEFAULT_WIRELESS_FE_PER_ITEM;
    private volatile double wirelessFePerItemPerBlock = DEFAULT_WIRELESS_FE_PER_ITEM_PER_BLOCK;
    private volatile double shuttleFePerBlock = DEFAULT_SHUTTLE_FE_PER_BLOCK;
    private volatile double wakeFePerChunkPerTick = DEFAULT_WAKE_FE_PER_CHUNK_PER_TICK;
    private volatile double craftingStationFePerTick = DEFAULT_CRAFTING_STATION_FE_PER_TICK;
    private volatile int rackBaysPerSection = DEFAULT_RACK_BAYS_PER_SECTION;
    private volatile int inboundBufferSlots = DEFAULT_INBOUND_BUFFER_SLOTS;
    private volatile int putAwayPerTick = DEFAULT_PUT_AWAY_PER_TICK;
    private volatile int cpuOrdersPerCpuBlock = DEFAULT_CPU_ORDERS_PER_CPU_BLOCK;
    private volatile int cpuStacksPerBufferBlock = DEFAULT_CPU_STACKS_PER_BUFFER_BLOCK;
    private volatile int cpuMaxBlocks = DEFAULT_CPU_MAX_BLOCKS;
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
    private volatile int fieldCheckInterval = DEFAULT_FIELD_CHECK_INTERVAL;
    private volatile int fieldRadius = DEFAULT_FIELD_RADIUS;
    private volatile int fieldMaxRadius = DEFAULT_FIELD_MAX_RADIUS;
    private volatile int fieldCorrosionWater = DEFAULT_FIELD_CORROSION_WATER;
    private volatile int fieldAntiCorrosionLevel = DEFAULT_FIELD_ANTI_CORROSION_LEVEL;
    private volatile int fertilizeInterval = DEFAULT_FERTILIZE_INTERVAL;
    private volatile int dosesPerPlot = DEFAULT_DOSES_PER_PLOT;
    private volatile int fertilizerReserve = DEFAULT_FERTILIZER_RESERVE;
    private volatile double fertilizeShare = DEFAULT_FERTILIZE_SHARE;

    private StorageStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        sourcesPerTick = intField(files.get(WAREHOUSE), "sources_per_tick", DEFAULT_SOURCES_PER_TICK);
        rackSlots = intField(files.get(RACK), "slots", DEFAULT_RACK_SLOTS);
        garageCapacity = intField(files.get(WAREHOUSE), "garage_capacity", DEFAULT_GARAGE_CAPACITY);
        railMaxSpan = intField(files.get(WAREHOUSE), "rail_max_span", DEFAULT_RAIL_MAX_SPAN);
        terminalOutputSlots = intField(files.get(WAREHOUSE), "terminal_output_slots", DEFAULT_TERMINAL_OUTPUT_SLOTS);
        pushInterval = intField(files.get(WAREHOUSE), "push_interval", DEFAULT_PUSH_INTERVAL);
        outboundCheckInterval = intField(files.get(WAREHOUSE), "outbound_check_interval", DEFAULT_OUTBOUND_CHECK_INTERVAL);
        craftingStationTicks = intField(files.get(CRAFTING_STATION), "work_ticks", DEFAULT_CRAFTING_STATION_TICKS);
        stockRuleInterval = intField(files.get(WAREHOUSE), "stock_rule_interval", DEFAULT_STOCK_RULE_INTERVAL);
        liftSpeed = doubleField(files.get(WAREHOUSE), "lift_speed", DEFAULT_LIFT_SPEED);
        liftHandoverTicks = intField(files.get(WAREHOUSE), "lift_handover_ticks", DEFAULT_LIFT_HANDOVER_TICKS);
        liftAccel = Math.max(0.0001, doubleField(files.get(WAREHOUSE), "lift_accel", DEFAULT_LIFT_ACCEL));
        liftUpgradeSlots = Math.max(0, intField(files.get(WAREHOUSE), "lift_upgrade_slots", DEFAULT_LIFT_UPGRADE_SLOTS));
        liftSpeedPerUpgrade = Math.max(0, doubleField(files.get(WAREHOUSE), "lift_speed_per_upgrade", DEFAULT_LIFT_SPEED_PER_UPGRADE));
        liftAccelPerUpgrade = Math.max(0, doubleField(files.get(WAREHOUSE), "lift_accel_per_upgrade", DEFAULT_LIFT_ACCEL_PER_UPGRADE));
        rackDisplayCount = intField(files.get(RACK), "display_count", DEFAULT_RACK_DISPLAY_COUNT);
        anchorUpgradeSlots = countField(files.get(WAREHOUSE), "anchor_upgrade_slots", DEFAULT_ANCHOR_UPGRADE_SLOTS);
        chunksPerUpgrade = intField(files.get(WAREHOUSE), "chunks_per_upgrade", DEFAULT_CHUNKS_PER_UPGRADE);
        wakeGraceTicks = countField(files.get(WAREHOUSE), "wake_grace_seconds", DEFAULT_WAKE_GRACE_SECONDS) * 20;
        siteSnapshotInterval = intField(files.get(WAREHOUSE), "site_snapshot_interval", DEFAULT_SITE_SNAPSHOT_INTERVAL);
        controllerEnergyCapacity = intField(files.get(POWER), "controller_energy_capacity", DEFAULT_CONTROLLER_ENERGY_CAPACITY);
        wirelessFePerItem = costField(files.get(POWER), "wireless_fe_per_item", DEFAULT_WIRELESS_FE_PER_ITEM);
        wirelessFePerItemPerBlock = costField(files.get(POWER), "wireless_fe_per_item_per_block", DEFAULT_WIRELESS_FE_PER_ITEM_PER_BLOCK);
        shuttleFePerBlock = costField(files.get(POWER), "shuttle_fe_per_block", DEFAULT_SHUTTLE_FE_PER_BLOCK);
        wakeFePerChunkPerTick = costField(files.get(POWER), "wake_fe_per_chunk_per_tick", DEFAULT_WAKE_FE_PER_CHUNK_PER_TICK);
        craftingStationFePerTick = costField(files.get(POWER), "crafting_station_fe_per_tick", DEFAULT_CRAFTING_STATION_FE_PER_TICK);
        rackBaysPerSection = intField(files.get(RACK), "bays_per_section", DEFAULT_RACK_BAYS_PER_SECTION);
        inboundBufferSlots = intField(files.get(WAREHOUSE), "inbound_buffer_slots", DEFAULT_INBOUND_BUFFER_SLOTS);
        putAwayPerTick = intField(files.get(WAREHOUSE), "put_away_per_tick", DEFAULT_PUT_AWAY_PER_TICK);
        cpuOrdersPerCpuBlock = intField(files.get(CPU), "orders_per_cpu_block", DEFAULT_CPU_ORDERS_PER_CPU_BLOCK);
        cpuStacksPerBufferBlock = intField(files.get(CPU), "stacks_per_buffer_block", DEFAULT_CPU_STACKS_PER_BUFFER_BLOCK);
        cpuMaxBlocks = intField(files.get(CPU), "max_blocks", DEFAULT_CPU_MAX_BLOCKS);
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
        fieldCheckInterval = intField(files.get(FIELD), "field_check_interval", DEFAULT_FIELD_CHECK_INTERVAL);
        fieldRadius = intField(files.get(FIELD), "radius", DEFAULT_FIELD_RADIUS);
        fieldMaxRadius = intField(files.get(FIELD), "max_radius", DEFAULT_FIELD_MAX_RADIUS);
        fieldCorrosionWater = intField(files.get(FIELD), "corrosion_water", DEFAULT_FIELD_CORROSION_WATER);
        fieldAntiCorrosionLevel = intField(files.get(FIELD), "anti_corrosion_level", DEFAULT_FIELD_ANTI_CORROSION_LEVEL);
        fertilizeInterval = intField(files.get(FIELD), "fertilize_interval", DEFAULT_FERTILIZE_INTERVAL);
        dosesPerPlot = intField(files.get(FIELD), "doses_per_plot", DEFAULT_DOSES_PER_PLOT);
        fertilizerReserve = countField(files.get(FIELD), "fertilizer_reserve", DEFAULT_FERTILIZER_RESERVE);
        fertilizeShare = Math.min(1.0, doubleField(files.get(FIELD), "fertilize_share", DEFAULT_FERTILIZE_SHARE));
        shuttleTiers = ShuttleStatsReader.read(files.get(SHUTTLE));
        ServusStorage.LOGGER.info("Warehouse polls {} sources per tick", sourcesPerTick);
    }

    private static double doubleField(JsonElement element, String field, double fallback) {
        if (element instanceof JsonObject object && object.has(field)) {
            return Math.max(0.01, object.get(field).getAsDouble());
        }
        return fallback;
    }

    /** A cost: 0 or more, fractions allowed. */
    private static double costField(JsonElement element, String field, double fallback) {
        if (element instanceof JsonObject object && object.has(field)) {
            return Math.max(0, object.get(field).getAsDouble());
        }
        return fallback;
    }

    /** Like {@link #intField} but 0 is allowed. */
    private static int countField(JsonElement element, String field, int fallback) {
        if (element instanceof JsonObject object && object.has(field)) {
            return Math.max(0, object.get(field).getAsInt());
        }
        return fallback;
    }

    private static int intField(JsonElement element, String field, int fallback) {
        if (element instanceof JsonObject object && object.has(field)) {
            return Math.max(1, object.get(field).getAsInt());
        }
        return fallback;
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

    /** How long a step's outputs wait at their producer for a busy later step before they go to storage. */
    public int handoffWaitTicks() {
        return handoffWaitTicks;
    }

    /** Order slots each Crafting CPU block adds to its formed CPU. */
    public int cpuOrdersPerCpuBlock() {
        return cpuOrdersPerCpuBlock;
    }

    /** Buffer stacks each CPU Buffer block adds to its formed CPU. */
    public int cpuStacksPerBufferBlock() {
        return cpuStacksPerBufferBlock;
    }

    /** The most blocks one CPU box may have; bounds the forming flood fill. */
    public int cpuMaxBlocks() {
        return cpuMaxBlocks;
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

    /** Blocks per tick the lift car of an elevator shaft moves. */
    public double liftSpeed() {
        return liftSpeed;
    }

    /** Ticks a shuttle stands still when it hands over to a lift car and when the car hands it back. */
    public int liftHandoverTicks() {
        return liftHandoverTicks;
    }

    /** Speed the lift car gains per tick when it starts and loses when it brakes. */
    public double liftAccel() {
        return liftAccel;
    }

    /** Speed upgrade slots of an elevator shaft. */
    public int liftUpgradeSlots() {
        return liftUpgradeSlots;
    }

    /** Blocks per tick each Speed upgrade adds to the lift car's top speed. */
    public double liftSpeedPerUpgrade() {
        return liftSpeedPerUpgrade;
    }

    /** What each Speed upgrade adds to the lift car's acceleration. */
    public double liftAccelPerUpgrade() {
        return liftAccelPerUpgrade;
    }

    /** Anchor Upgrade slots of a Warehouse Controller. */
    public int anchorUpgradeSlots() {
        return anchorUpgradeSlots;
    }

    /** Chunks one Anchor Upgrade covers. */
    public int chunksPerUpgrade() {
        return chunksPerUpgrade;
    }

    /** FE a Warehouse Controller holds ({@code storage/power.json}). */
    public int controllerEnergyCapacity() {
        return controllerEnergyCapacity;
    }

    /** FE every item of a wireless transfer costs. */
    public double wirelessFePerItem() {
        return wirelessFePerItem;
    }

    /** FE every item costs per block between the two controllers. */
    public double wirelessFePerItemPerBlock() {
        return wirelessFePerItemPerBlock;
    }

    /** For GameTests: the two FE numbers of wireless transfers. */
    public void setWirelessCost(double perItem, double perItemPerBlock) {
        wirelessFePerItem = Math.max(0, perItem);
        wirelessFePerItemPerBlock = Math.max(0, perItemPerBlock);
    }

    /** FE a shuttle pays for every block it starts to drive, and per block of height of a lift ride. */
    public double shuttleFePerBlock() {
        return shuttleFePerBlock;
    }

    /** FE a woken site pays per chunk for every tick a wake holds it. */
    public double wakeFePerChunkPerTick() {
        return wakeFePerChunkPerTick;
    }

    /** FE a Crafting Station pays for every tick it works on a craft. */
    public double craftingStationFePerTick() {
        return craftingStationFePerTick;
    }

    /** For GameTests: the warehouse power costs of shuttles, wakes and Crafting Stations. */
    public void setWarehousePowerCost(double shuttlePerBlock, double wakePerChunkPerTick, double craftingPerTick) {
        shuttleFePerBlock = Math.max(0, shuttlePerBlock);
        wakeFePerChunkPerTick = Math.max(0, wakePerChunkPerTick);
        craftingStationFePerTick = Math.max(0, craftingPerTick);
    }

    /** Ticks a woken site stays loaded after its work is done. */
    public int wakeGraceTicks() {
        return wakeGraceTicks;
    }

    /** For GameTests: the grace time in ticks. */
    public void setWakeGraceTicks(int ticks) {
        wakeGraceTicks = Math.max(0, ticks);
    }

    /** Ticks between two stock reports of an online site. */
    public int siteSnapshotInterval() {
        return siteSnapshotInterval;
    }

    /** For GameTests: ticks between two stock reports. */
    public void setSiteSnapshotInterval(int ticks) {
        siteSnapshotInterval = Math.max(1, ticks);
    }

    /** For GameTests: chunks one upgrade covers. */
    public void setChunksPerUpgrade(int chunks) {
        chunksPerUpgrade = Math.max(1, chunks);
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

    /** Ticks between two looks of a warehouse at the Field Heads of its fields. */
    public int fieldCheckInterval() {
        return fieldCheckInterval;
    }

    /** For tests: sets the field check interval as if a datapack had been loaded. */
    public void setFieldCheckInterval(int ticks) {
        fieldCheckInterval = ticks;
    }

    /** Blocks a field reaches to each side of its rails with no Range Upgrade. */
    public int fieldRadius() {
        return fieldRadius;
    }

    /** The largest field radius Range Upgrades reach. */
    public int fieldMaxRadius() {
        return fieldMaxRadius;
    }

    /** mB of hydration water per aging step of a field's copper rails. */
    public int fieldCorrosionWater() {
        return fieldCorrosionWater;
    }

    /** How many anti-corrosion items the data line keeps in a Field Head. */
    public int fieldAntiCorrosionLevel() {
        return fieldAntiCorrosionLevel;
    }

    /** Ticks a field waits at least between two calls for a Spreader. */
    public int fertilizeInterval() {
        return fertilizeInterval;
    }

    /** For tests: sets the fertilize interval as if a datapack had been loaded. */
    public void setFertilizeInterval(int ticks) {
        fertilizeInterval = ticks;
    }

    /** Doses a plot gets in one round of a Spreader. */
    public int dosesPerPlot() {
        return dosesPerPlot;
    }

    /** For tests: sets the doses per plot as if a datapack had been loaded. */
    public void setDosesPerPlot(int doses) {
        dosesPerPlot = doses;
    }

    /** The Keep in stock reserve of a Field Head that was never set. */
    public int fertilizerReserve() {
        return fertilizerReserve;
    }

    /** The ripe share below which a head in Until ripe share mode calls a Spreader, until set per head. */
    public double fertilizeShare() {
        return fertilizeShare;
    }

    /** Shuttle speed, stop times and tiers from {@code shuttle.json}. */
    public ShuttleTiers shuttleTiers() {
        return shuttleTiers;
    }

    /** For tests: sets the shuttle stats as if a datapack had been loaded. */
    public void setShuttleTiers(ShuttleTiers tiers) {
        shuttleTiers = tiers;
    }
}
