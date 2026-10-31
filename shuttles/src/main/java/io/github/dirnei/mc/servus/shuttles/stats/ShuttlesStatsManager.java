package io.github.dirnei.mc.servus.shuttles.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.shuttles.ServusShuttles;
import io.github.dirnei.mc.servus.shuttles.shuttle.TravelWeights;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads storage block stats from {@code data/<ns>/storage/<name>.json}. */
public final class ShuttlesStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "shuttles_stats_manager");

    public static final String DIRECTORY = "shuttles";
    public static final int DEFAULT_SOURCES_PER_TICK = 4;
    public static final int DEFAULT_RACK_SLOTS = 27;
    public static final int DEFAULT_GARAGE_CAPACITY = 4;
    public static final int DEFAULT_RAIL_MAX_SPAN = 3;
    public static final int DEFAULT_TERMINAL_OUTPUT_SLOTS = 9;
    public static final int DEFAULT_PUSH_INTERVAL = 8;
    public static final int DEFAULT_REFUSED_SHIPMENT_TIMEOUT = 1200;
    public static final int DEFAULT_OUTBOUND_CHECK_INTERVAL = 40;
    public static final int DEFAULT_CRAFTING_STATION_TICKS = 40;
    public static final int DEFAULT_STOCK_RULE_INTERVAL = 100;
    public static final double DEFAULT_LIFT_SPEED = 0.1;
    public static final int DEFAULT_LIFT_HANDOVER_TICKS = 10;
    public static final int DEFAULT_DOOR_TICKS = 10;
    public static final double DEFAULT_LIFT_ACCEL = 0.005;
    public static final int DEFAULT_LIFT_UPGRADE_SLOTS = 4;
    public static final double DEFAULT_LIFT_SPEED_PER_UPGRADE = 0.05;
    public static final double DEFAULT_LIFT_ACCEL_PER_UPGRADE = 0.0025;
    public static final int DEFAULT_RACK_DISPLAY_COUNT = 4;
    public static final int DEFAULT_RACK_POTATO_TOP_ITEMS = 5;
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
    public static final int DEFAULT_RACK_BAYS_PER_SHELF = 3;
    public static final int DEFAULT_INBOUND_BUFFER_SLOTS = 9;
    public static final int DEFAULT_PUT_AWAY_PER_TICK = 1;
    public static final int DEFAULT_CPU_ORDERS_PER_CPU_BLOCK = 1;
    public static final int DEFAULT_CPU_STACKS_PER_BUFFER_BLOCK = 1;
    public static final int DEFAULT_CPU_MAX_BLOCKS = 512;
    public static final int DEFAULT_HANDOFF_WAIT_TICKS = 100;
    public static final int DEFAULT_PATTERNS_PER_BLOCK = 9;
    public static final double DEFAULT_PACKING_ITEMS_PER_SECOND = 10.0;
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
    public static final int DEFAULT_FIELD_CORROSION_WATER = 9000;
    public static final int DEFAULT_AXE_DAMAGE_PER_RAIL = 2;
    public static final int DEFAULT_FIELD_ANTI_CORROSION_LEVEL = 8;
    public static final int DEFAULT_FERTILIZE_INTERVAL = 600;
    public static final int DEFAULT_DOSES_PER_PLOT = 2;
    public static final int DEFAULT_FERTILIZER_RESERVE = 64;
    public static final int DEFAULT_ANTI_CORROSION_RESERVE = 0;
    public static final double DEFAULT_FERTILIZE_SHARE = 0.5;
    public static final int DEFAULT_SEED_BUFFER = 64;
    public static final int DEFAULT_FERTILIZER_BUFFER = 64;
    public static final int DEFAULT_HARVEST_BUFFER_SLOTS = 9;
    public static final int DEFAULT_FELL_TICKS = 40;
    public static final int DEFAULT_TOPPLE_TICKS = 80;
    public static final int DEFAULT_UNFOLD_TICKS = 30;
    public static final int DEFAULT_MAX_TREE_LOGS = 200;
    public static final int DEFAULT_MAX_TREE_BLOCKS = 1000;
    public static final int DEFAULT_HARVEST_LIMIT = 0;
    public static final int DEFAULT_FERTILIZE_BELOW = 0;
    public static final int DEFAULT_TANKER_PUMP_TICKS = 40;
    public static final int DEFAULT_FLUID_RACK_CAPACITY_PER_BAY = 16000;
    public static final int DEFAULT_FLUID_RACK_BAYS_PER_SHELF = 3;
    public static final int DEFAULT_FLUID_RACK_LEVEL_STEPS = 16;
    public static final int DEFAULT_FLUID_CHECK_INTERVAL = 40;
    public static final int DEFAULT_TANKER_MIN_LOAD = 1000;
    public static final int DEFAULT_FLUID_STATION_TANK_CAPACITY = 16000;

    public static final int DEFAULT_HANDHELD_FIND_GLOW_SECONDS = 8;
    public static final int DEFAULT_HANDHELD_SCAN_RANGE = 16;
    public static final int DEFAULT_HANDHELD_ORDER_MARGIN = 64;
    public static final int DEFAULT_LOCKER_BOX_S = 3;
    public static final int DEFAULT_LOCKER_BOX_M = 6;
    public static final int DEFAULT_LOCKER_BOX_L = 9;
    public static final int DEFAULT_LOCKER_RETURN_TICKS = 24000;
    public static final int DEFAULT_LOCKER_CHECK_INTERVAL = 20;

    private static final double DEFAULT_FREE_SLOT_BUCKET = 0.25;
    private static final double DEFAULT_FOLLOW_GAP = 1.0;
    private static final double DEFAULT_TRAFFIC_PER_SHUTTLE = 4;
    private static final Identifier WAREHOUSE = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "warehouse");
    private static final Identifier RACK = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "rack");
    private static final Identifier CRAFTING_STATION = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "crafting_station");
    private static final Identifier CPU = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "cpu");
    private static final Identifier PACKING_STATION = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "packing_station");
    private static final Identifier PATTERN_STORAGE = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "pattern_storage");
    private static final Identifier SHUTTLE = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "shuttle");
    private static final Identifier FIELD = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "field");
    private static final Identifier POWER = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "power");
    private static final Identifier FLUID_RACK = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "fluid_rack");

    public static final ShuttlesStatsManager INSTANCE = new ShuttlesStatsManager();

    private volatile int sourcesPerTick = DEFAULT_SOURCES_PER_TICK;
    private volatile int rackSlots = DEFAULT_RACK_SLOTS;
    private volatile int garageCapacity = DEFAULT_GARAGE_CAPACITY;
    private volatile ShuttleTiers shuttleTiers = ShuttleTiers.DEFAULT;
    private volatile int railMaxSpan = DEFAULT_RAIL_MAX_SPAN;
    private volatile int terminalOutputSlots = DEFAULT_TERMINAL_OUTPUT_SLOTS;
    private volatile int pushInterval = DEFAULT_PUSH_INTERVAL;
    private volatile int refusedShipmentTimeout = DEFAULT_REFUSED_SHIPMENT_TIMEOUT;
    private volatile int outboundCheckInterval = DEFAULT_OUTBOUND_CHECK_INTERVAL;
    private volatile int craftingStationTicks = DEFAULT_CRAFTING_STATION_TICKS;
    private volatile int stockRuleInterval = DEFAULT_STOCK_RULE_INTERVAL;
    private volatile double liftSpeed = DEFAULT_LIFT_SPEED;
    private volatile int liftHandoverTicks = DEFAULT_LIFT_HANDOVER_TICKS;
    private volatile int doorTicks = DEFAULT_DOOR_TICKS;
    private volatile double liftAccel = DEFAULT_LIFT_ACCEL;
    private volatile int liftUpgradeSlots = DEFAULT_LIFT_UPGRADE_SLOTS;
    private volatile double liftSpeedPerUpgrade = DEFAULT_LIFT_SPEED_PER_UPGRADE;
    private volatile double liftAccelPerUpgrade = DEFAULT_LIFT_ACCEL_PER_UPGRADE;
    private volatile int rackDisplayCount = DEFAULT_RACK_DISPLAY_COUNT;
    private volatile int rackPotatoTopItems = DEFAULT_RACK_POTATO_TOP_ITEMS;
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
    private volatile int rackBaysPerShelf = DEFAULT_RACK_BAYS_PER_SHELF;
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
    private volatile int axeDamagePerRail = DEFAULT_AXE_DAMAGE_PER_RAIL;
    private volatile int fieldAntiCorrosionLevel = DEFAULT_FIELD_ANTI_CORROSION_LEVEL;
    private volatile int fertilizeInterval = DEFAULT_FERTILIZE_INTERVAL;
    private volatile int dosesPerPlot = DEFAULT_DOSES_PER_PLOT;
    private volatile int fertilizerReserve = DEFAULT_FERTILIZER_RESERVE;
    private volatile int antiCorrosionReserve = DEFAULT_ANTI_CORROSION_RESERVE;
    private volatile double fertilizeShare = DEFAULT_FERTILIZE_SHARE;
    private volatile int seedBuffer = DEFAULT_SEED_BUFFER;
    private volatile int fertilizerBuffer = DEFAULT_FERTILIZER_BUFFER;
    private volatile int harvestBufferSlots = DEFAULT_HARVEST_BUFFER_SLOTS;
    private volatile int fellTicks = DEFAULT_FELL_TICKS;
    private volatile int toppleTicks = DEFAULT_TOPPLE_TICKS;
    private volatile int unfoldTicks = DEFAULT_UNFOLD_TICKS;
    private volatile int maxTreeLogs = DEFAULT_MAX_TREE_LOGS;
    private volatile int maxTreeBlocks = DEFAULT_MAX_TREE_BLOCKS;
    private volatile int harvestLimit = DEFAULT_HARVEST_LIMIT;
    private volatile int fertilizeBelow = DEFAULT_FERTILIZE_BELOW;
    private volatile int tankerPumpTicks = DEFAULT_TANKER_PUMP_TICKS;
    private volatile int fluidRackCapacityPerBay = DEFAULT_FLUID_RACK_CAPACITY_PER_BAY;
    private volatile int fluidRackBaysPerShelf = DEFAULT_FLUID_RACK_BAYS_PER_SHELF;
    private volatile int fluidRackLevelSteps = DEFAULT_FLUID_RACK_LEVEL_STEPS;
    private volatile int fluidCheckInterval = DEFAULT_FLUID_CHECK_INTERVAL;
    private volatile int tankerMinLoad = DEFAULT_TANKER_MIN_LOAD;
    private volatile int fluidStationTankCapacity = DEFAULT_FLUID_STATION_TANK_CAPACITY;
    private volatile TravelWeights travelWeights = TravelWeights.DEFAULT;
    private volatile int handheldFindGlowSeconds = DEFAULT_HANDHELD_FIND_GLOW_SECONDS;
    private volatile int handheldScanRange = DEFAULT_HANDHELD_SCAN_RANGE;
    private volatile int handheldOrderMargin = DEFAULT_HANDHELD_ORDER_MARGIN;
    private volatile LockerLayout.Capacities lockerCapacities =
            new LockerLayout.Capacities(DEFAULT_LOCKER_BOX_S, DEFAULT_LOCKER_BOX_M, DEFAULT_LOCKER_BOX_L);
    private volatile int lockerReturnTicks = DEFAULT_LOCKER_RETURN_TICKS;
    private volatile int lockerCheckInterval = DEFAULT_LOCKER_CHECK_INTERVAL;
    private volatile double freeSlotBucket = DEFAULT_FREE_SLOT_BUCKET;
    private volatile double followGap = DEFAULT_FOLLOW_GAP;
    private volatile double trafficPerShuttle = DEFAULT_TRAFFIC_PER_SHUTTLE;

    private ShuttlesStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        sourcesPerTick = intField(files.get(WAREHOUSE), "sources_per_tick", DEFAULT_SOURCES_PER_TICK);
        rackSlots = intField(files.get(RACK), "slots", DEFAULT_RACK_SLOTS);
        garageCapacity = intField(files.get(WAREHOUSE), "garage_capacity", DEFAULT_GARAGE_CAPACITY);
        railMaxSpan = intField(files.get(WAREHOUSE), "rail_max_span", DEFAULT_RAIL_MAX_SPAN);
        handheldFindGlowSeconds = intField(files.get(WAREHOUSE), "handheld_find_glow_seconds", DEFAULT_HANDHELD_FIND_GLOW_SECONDS);
        handheldScanRange = Math.max(1, intField(files.get(WAREHOUSE), "handheld_scan_range", DEFAULT_HANDHELD_SCAN_RANGE));
        handheldOrderMargin = countField(files.get(WAREHOUSE), "handheld_order_margin", DEFAULT_HANDHELD_ORDER_MARGIN);
        lockerCapacities = new LockerLayout.Capacities(intField(files.get(WAREHOUSE), "locker_box_s", DEFAULT_LOCKER_BOX_S),
                intField(files.get(WAREHOUSE), "locker_box_m", DEFAULT_LOCKER_BOX_M),
                intField(files.get(WAREHOUSE), "locker_box_l", DEFAULT_LOCKER_BOX_L));
        lockerReturnTicks = intField(files.get(WAREHOUSE), "locker_return_ticks", DEFAULT_LOCKER_RETURN_TICKS);
        lockerCheckInterval = intField(files.get(WAREHOUSE), "locker_check_interval", DEFAULT_LOCKER_CHECK_INTERVAL);
        terminalOutputSlots = intField(files.get(WAREHOUSE), "terminal_output_slots", DEFAULT_TERMINAL_OUTPUT_SLOTS);
        pushInterval = intField(files.get(WAREHOUSE), "push_interval", DEFAULT_PUSH_INTERVAL);
        refusedShipmentTimeout = Math.max(pushInterval,
                intField(files.get(WAREHOUSE), "refused_shipment_timeout", DEFAULT_REFUSED_SHIPMENT_TIMEOUT));
        outboundCheckInterval = intField(files.get(WAREHOUSE), "outbound_check_interval", DEFAULT_OUTBOUND_CHECK_INTERVAL);
        craftingStationTicks = intField(files.get(CRAFTING_STATION), "work_ticks", DEFAULT_CRAFTING_STATION_TICKS);
        stockRuleInterval = intField(files.get(WAREHOUSE), "stock_rule_interval", DEFAULT_STOCK_RULE_INTERVAL);
        liftSpeed = doubleField(files.get(WAREHOUSE), "lift_speed", DEFAULT_LIFT_SPEED);
        liftHandoverTicks = intField(files.get(WAREHOUSE), "lift_handover_ticks", DEFAULT_LIFT_HANDOVER_TICKS);
        doorTicks = countField(files.get(WAREHOUSE), "door_ticks", DEFAULT_DOOR_TICKS);
        liftAccel = Math.max(0.0001, doubleField(files.get(WAREHOUSE), "lift_accel", DEFAULT_LIFT_ACCEL));
        liftUpgradeSlots = Math.max(0, intField(files.get(WAREHOUSE), "lift_upgrade_slots", DEFAULT_LIFT_UPGRADE_SLOTS));
        liftSpeedPerUpgrade = Math.max(0, doubleField(files.get(WAREHOUSE), "lift_speed_per_upgrade", DEFAULT_LIFT_SPEED_PER_UPGRADE));
        liftAccelPerUpgrade = Math.max(0, doubleField(files.get(WAREHOUSE), "lift_accel_per_upgrade", DEFAULT_LIFT_ACCEL_PER_UPGRADE));
        rackDisplayCount = intField(files.get(RACK), "display_count", DEFAULT_RACK_DISPLAY_COUNT);
        rackPotatoTopItems = intField(files.get(RACK), "potato_top_items", DEFAULT_RACK_POTATO_TOP_ITEMS);
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
        rackBaysPerShelf = baysPerShelf(files.get(RACK), DEFAULT_RACK_BAYS_PER_SHELF);
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
        axeDamagePerRail = countField(files.get(FIELD), "axe_damage_per_rail", DEFAULT_AXE_DAMAGE_PER_RAIL);
        fieldAntiCorrosionLevel = intField(files.get(FIELD), "anti_corrosion_level", DEFAULT_FIELD_ANTI_CORROSION_LEVEL);
        fertilizeInterval = intField(files.get(FIELD), "fertilize_interval", DEFAULT_FERTILIZE_INTERVAL);
        dosesPerPlot = intField(files.get(FIELD), "doses_per_plot", DEFAULT_DOSES_PER_PLOT);
        fertilizerReserve = countField(files.get(FIELD), "fertilizer_reserve", DEFAULT_FERTILIZER_RESERVE);
        antiCorrosionReserve = countField(files.get(FIELD), "anti_corrosion_reserve", DEFAULT_ANTI_CORROSION_RESERVE);
        fertilizeShare = Math.min(1.0, doubleField(files.get(FIELD), "fertilize_share", DEFAULT_FERTILIZE_SHARE));
        seedBuffer = countField(files.get(FIELD), "seed_buffer", DEFAULT_SEED_BUFFER);
        fertilizerBuffer = countField(files.get(FIELD), "fertilizer_buffer", DEFAULT_FERTILIZER_BUFFER);
        harvestBufferSlots = intField(files.get(FIELD), "harvest_buffer_slots", DEFAULT_HARVEST_BUFFER_SLOTS);
        fellTicks = intField(files.get(FIELD), "fell_ticks", DEFAULT_FELL_TICKS);
        toppleTicks = intField(files.get(FIELD), "topple_ticks", DEFAULT_TOPPLE_TICKS);
        unfoldTicks = intField(files.get(FIELD), "unfold_ticks", DEFAULT_UNFOLD_TICKS);
        maxTreeLogs = intField(files.get(FIELD), "max_tree_logs", DEFAULT_MAX_TREE_LOGS);
        maxTreeBlocks = intField(files.get(FIELD), "max_tree_blocks", DEFAULT_MAX_TREE_BLOCKS);
        harvestLimit = countField(files.get(FIELD), "harvest_limit", DEFAULT_HARVEST_LIMIT);
        fertilizeBelow = countField(files.get(FIELD), "fertilize_below", DEFAULT_FERTILIZE_BELOW);
        tankerPumpTicks = intField(files.get(WAREHOUSE), "tanker_pump_ticks", DEFAULT_TANKER_PUMP_TICKS);
        fluidRackCapacityPerBay = intField(files.get(FLUID_RACK), "capacity_per_bay", DEFAULT_FLUID_RACK_CAPACITY_PER_BAY);
        fluidRackBaysPerShelf = baysPerShelf(files.get(FLUID_RACK), DEFAULT_FLUID_RACK_BAYS_PER_SHELF);
        fluidRackLevelSteps = intField(files.get(FLUID_RACK), "level_steps", DEFAULT_FLUID_RACK_LEVEL_STEPS);
        fluidCheckInterval = intField(files.get(WAREHOUSE), "fluid_check_interval", DEFAULT_FLUID_CHECK_INTERVAL);
        tankerMinLoad = countField(files.get(WAREHOUSE), "tanker_min_load", DEFAULT_TANKER_MIN_LOAD);
        fluidStationTankCapacity = intField(files.get(WAREHOUSE), "fluid_station_tank_capacity", DEFAULT_FLUID_STATION_TANK_CAPACITY);
        travelWeights = travelWeights(files.get(WAREHOUSE));
        freeSlotBucket = Math.min(1.0, doubleField(files.get(WAREHOUSE), "free_slot_bucket", DEFAULT_FREE_SLOT_BUCKET));
        followGap = doubleField(files.get(WAREHOUSE), "follow_gap", DEFAULT_FOLLOW_GAP);
        JsonElement costs = files.get(WAREHOUSE) instanceof JsonObject warehouse ? warehouse.get("travel_costs") : null;
        trafficPerShuttle = costField(costs, "traffic_per_shuttle", DEFAULT_TRAFFIC_PER_SHUTTLE);
        shuttleTiers = ShuttleStatsReader.read(files.get(SHUTTLE));
        ServusShuttles.LOGGER.info("Warehouse polls {} sources per tick", sourcesPerTick);
    }

    /** The travel cost weights from the {@code travel_costs} object; missing fields keep their defaults. */
    private static TravelWeights travelWeights(JsonElement warehouse) {
        JsonElement costs = warehouse instanceof JsonObject object ? object.get("travel_costs") : null;
        TravelWeights base = TravelWeights.DEFAULT;
        return new TravelWeights(costField(costs, "rail_block", base.railBlock()), costField(costs, "corner", base.corner()),
                costField(costs, "lift_block", base.liftBlock()), costField(costs, "lift_handover", base.handover()),
                costField(costs, "stop", base.stop()), costField(costs, "lift_wait_per_job", base.liftWaitPerJob()));
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

    /** {@code bays_per_shelf}, or the older {@code bays_per_section} when a pack still uses it. */
    private static int baysPerShelf(JsonElement element, int fallback) {
        return intField(element, "bays_per_shelf", intField(element, "bays_per_section", fallback));
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

    /** Ticks a Packing Station shipment may stay cut from its target, or refused by a machine target, before it ends short. */
    public int refusedShipmentTimeout() {
        return refusedShipmentTimeout;
    }

    /** For GameTests: the refusal timeout in ticks, at least the push interval. */
    public void setRefusedShipmentTimeout(int ticks) {
        refusedShipmentTimeout = Math.max(pushInterval, ticks);
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

    /** Blocks per tick the lift car of an elevator shaft moves. */
    public double liftSpeed() {
        return liftSpeed;
    }

    /** Ticks a shuttle stands still when it hands over to a lift car and when the car hands it back. */
    public int liftHandoverTicks() {
        return liftHandoverTicks;
    }

    /** Ticks a shuttle waits inside its garage while the door opens before it drives out, {@code door_ticks} in {@code warehouse.json}. */
    public int doorTicks() {
        return doorTicks;
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

    /** How many items the Data Potato lists for a rack shelf. */
    public int rackPotatoTopItems() {
        return rackPotatoTopItems;
    }

    /** How long the shelves a handheld find points at glow, in seconds. */
    public int handheldFindGlowSeconds() {
        return handheldFindGlowSeconds;
    }

    /** How far the handheld scans a shelf, in blocks; a client on a remote server uses the default. */
    public int handheldScanRange() {
        return handheldScanRange;
    }

    public int rackBaysPerShelf() {
        return rackBaysPerShelf;
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

    /** Axe durability that scraping a field costs per farm rail. */
    public int axeDamagePerRail() {
        return axeDamagePerRail;
    }

    /** How many anti-corrosion items supply runs keep in a Field Head's buffer. */
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

    /** The anti-corrosion Keep in stock reserve of a Field Head that was never set. */
    public int antiCorrosionReserve() {
        return antiCorrosionReserve;
    }

    /** The ripe share below which a head in Until ripe share mode calls a Spreader, until set per head. */
    public double fertilizeShare() {
        return fertilizeShare;
    }

    /** How many seeds of its crop supply runs keep in a Field Head's buffer; 0 turns the part off. */
    public int seedBuffer() {
        return seedBuffer;
    }

    /** For tests: sets the seed buffer level as if a datapack had been loaded. */
    public void setSeedBuffer(int level) {
        seedBuffer = level;
    }

    /** How many slots a Field Head's To storage buffer has for what its field shuttles bring back. */
    public int harvestBufferSlots() {
        return harvestBufferSlots;
    }

    /** Ticks a Lumber shuttle takes to fell a tree before Handling shortens it. */
    public int fellTicks() {
        return fellTicks;
    }

    /** For tests: sets the felling ticks as if a datapack had been loaded. */
    public void setFellTicks(int ticks) {
        fellTicks = ticks;
    }

    /** Ticks a felled tree takes to topple, break apart and the head to swing back over the hold; Handling never shortens it. */
    public int toppleTicks() {
        return toppleTicks;
    }

    /** For tests: sets the topple ticks as if a datapack had been loaded. */
    public void setToppleTicks(int ticks) {
        toppleTicks = ticks;
    }

    /** Ticks a Lumber shuttle takes to unfold its crane at a rail, and again to fold it; Handling never shortens them. */
    public int unfoldTicks() {
        return unfoldTicks;
    }

    /** For tests: sets the unfold ticks as if a datapack had been loaded. */
    public void setUnfoldTicks(int ticks) {
        unfoldTicks = ticks;
    }

    /** The most logs a tree may have for a Lumber shuttle to fell it. */
    public int maxTreeLogs() {
        return maxTreeLogs;
    }

    /** For tests: sets the log limit as if a datapack had been loaded. */
    public void setMaxTreeLogs(int logs) {
        maxTreeLogs = logs;
    }

    /** The most blocks in all, logs, leaves and hanging parts, a tree may have for a Lumber shuttle to fell it. */
    public int maxTreeBlocks() {
        return maxTreeBlocks;
    }

    /** How many items of its fertilizer supply runs keep in a Field Head's buffer; 0 turns the part off. */
    public int fertilizerBuffer() {
        return fertilizerBuffer;
    }

    /** For tests: sets the fertilizer buffer level as if a datapack had been loaded. */
    public void setFertilizerBuffer(int level) {
        fertilizerBuffer = level;
    }

    /** The harvest limit of a Field Head that was never set, in items of its produce; 0 for none. */
    public int harvestLimit() {
        return harvestLimit;
    }

    /** The fertilize level of a Field Head that was never set, in items of its produce; 0 for none. */
    public int fertilizeBelow() {
        return fertilizeBelow;
    }

    /** Shuttle speed, stop times and tiers from {@code shuttle.json}. */
    public ShuttleTiers shuttleTiers() {
        return shuttleTiers;
    }

    /** Ticks a tanker stops at a fluid store to take or put fluid, before Handling. */
    public int tankerPumpTicks() {
        return tankerPumpTicks;
    }

    /** mB one Fluid Rack bay adds to its shelf's tank. */
    public int fluidRackCapacityPerBay() {
        return fluidRackCapacityPerBay;
    }

    /** Bays a Fluid Rack shelf may hold. */
    public int fluidRackBaysPerShelf() {
        return fluidRackBaysPerShelf;
    }

    /** Steps in which clients learn the fill of a Fluid Rack shelf. */
    public int fluidRackLevelSteps() {
        return fluidRackLevelSteps;
    }

    /** Ticks between two looks of a warehouse at its fluid stations. */
    public int fluidCheckInterval() {
        return fluidCheckInterval;
    }

    /** Blocks around a site's chunks from which a Handheld HH-1 still orders. */
    public int handheldOrderMargin() {
        return handheldOrderMargin;
    }

    /** How many stacks each box size of a Parcel Locker holds. */
    public LockerLayout.Capacities lockerCapacities() {
        return lockerCapacities;
    }

    /** Ticks a ready parcel waits in its locker before it goes back to the racks. */
    public int lockerReturnTicks() {
        return lockerReturnTicks;
    }

    /** Ticks between two looks of a warehouse at its Parcel Lockers. */
    public int lockerCheckInterval() {
        return lockerCheckInterval;
    }

    /** mB a Fluid Inbound tank holds before a tanker run starts for it, unless it is full. */
    public int tankerMinLoad() {
        return tankerMinLoad;
    }

    /** mB the buffer tank of a fluid station holds. */
    public int fluidStationTankCapacity() {
        return fluidStationTankCapacity;
    }

    /** Travel time weights of rail paths, from {@code travel_costs} in {@code warehouse.json}. */
    public TravelWeights travelWeights() {
        return travelWeights;
    }

    /** The share of a bay's slots one free slot bucket of the put-away rule spans. */
    public double freeSlotBucket() {
        return freeSlotBucket;
    }

    /** Blocks from centre to centre a shuttle keeps behind the one ahead in its lane, {@code follow_gap} in {@code warehouse.json}. */
    public double followGap() {
        return followGap;
    }

    /** Ticks a shuttle on a step adds to the travel cost of that step, {@code travel_costs.traffic_per_shuttle}. */
    public double trafficPerShuttle() {
        return trafficPerShuttle;
    }
}
