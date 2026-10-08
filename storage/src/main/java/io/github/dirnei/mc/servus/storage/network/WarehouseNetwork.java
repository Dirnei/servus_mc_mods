package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.shuttle.RequestPlan;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import java.util.function.Predicate;
import io.github.dirnei.mc.servus.storage.index.CandidateNeighbours;
import io.github.dirnei.mc.servus.storage.index.WarehouseIndex;
import io.github.dirnei.mc.servus.storage.shuttle.PickJob;
import io.github.dirnei.mc.servus.storage.shuttle.PutAwayJob;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleFleet;
import io.github.dirnei.mc.servus.storage.shuttle.Shafts;
import io.github.dirnei.mc.servus.storage.shuttle.Shafts.Shaft;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.order.StockRule;
import io.github.dirnei.mc.servus.storage.order.StockRules;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import io.github.dirnei.mc.servus.storage.board.MachineRow;
import io.github.dirnei.mc.servus.storage.board.OrderRow;
import io.github.dirnei.mc.servus.storage.board.StepRow;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan;
import io.github.dirnei.mc.servus.storage.zone.ZoneDefinition;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * One connected set of storage blocks plus the inventories touching them.
 * Keeps an index of everything they hold, refreshed in batches, and offers
 * extract and insert over all of it.
 */
public final class WarehouseNetwork extends ServusNetwork<StorageNodeBlockEntity, WarehouseEvent> implements ShuttleFleet.Host {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "warehouse");

    public static final NetworkType<StorageNodeBlockEntity, WarehouseEvent, WarehouseNetwork> TYPE =
            new NetworkType<>(ID, (from, fromNode, side, to, toNode) ->
                    WarehouseConnectionRule.accepts(fromNode.kind(), side, toNode.kind())
                            && railSideAccepted(fromNode, side, toNode), WarehouseNetwork::new);

    public enum Status { ONLINE, NO_CONTROLLER, MULTIPLE_CONTROLLERS }

    /** A stock node may limit which of its sides take a rail; the rail side is asked from the stock node's view. */
    private static boolean railSideAccepted(StorageNodeBlockEntity from, io.github.dirnei.mc.servus.foundation.graph.Side side,
                                            StorageNodeBlockEntity to) {
        boolean fromRail = from.kind() == WarehouseConnectionRule.NodeKind.RAIL;
        boolean toRail = to.kind() == WarehouseConnectionRule.NodeKind.RAIL;
        if (fromRail == toRail) {
            return true;
        }
        if (toRail) {
            return !from.docksRail() || from.acceptsRail(side);
        }
        return !to.docksRail() || to.acceptsRail(opposite(side));
    }

    private static io.github.dirnei.mc.servus.foundation.graph.Side opposite(io.github.dirnei.mc.servus.foundation.graph.Side side) {
        return switch (side) {
            case UP -> io.github.dirnei.mc.servus.foundation.graph.Side.DOWN;
            case DOWN -> io.github.dirnei.mc.servus.foundation.graph.Side.UP;
            case NORTH -> io.github.dirnei.mc.servus.foundation.graph.Side.SOUTH;
            case SOUTH -> io.github.dirnei.mc.servus.foundation.graph.Side.NORTH;
            case WEST -> io.github.dirnei.mc.servus.foundation.graph.Side.EAST;
            case EAST -> io.github.dirnei.mc.servus.foundation.graph.Side.WEST;
        };
    }

    private record Source(BlockPos pos, boolean node, BlockCapabilityCache<IItemHandler, Direction> cache) {
    }

    private final Map<BlockPos, Source> sources = new LinkedHashMap<>();
    /** Members that hold no stock, such as rails. Never sources, never surrounded by candidates. */
    private final Set<BlockPos> connectors = new HashSet<>();
    /** Elevator segments, a subset of the connectors; grouped into shafts on the tick after a change. */
    private final Set<NodePos> elevators = new HashSet<>();
    private Map<NodePos, Shaft> shaftFloors = Map.of();
    private boolean shaftsDirty;
    /** The zone each zoned stock node carries. Unzoned nodes and foreign inventories are absent. */
    private final Map<BlockPos, ZoneDefinition> zoneOf = new LinkedHashMap<>();
    /** Set when a zone or rule changed; stations with waiting stacks retry on the next tick. */
    private boolean zonesDirty;
    /** Inbound stations: members that are neither sources nor travel nodes. */
    private final Set<BlockPos> stations = new HashSet<>();
    /** Shuttle garages: members that are neither sources nor travel nodes. */
    private final Set<BlockPos> garages = new HashSet<>();
    /** Counter terminals, for retrying requests with missing stock. */
    private final Set<BlockPos> terminals = new HashSet<>();
    private static final int MISSING_RETRY_TICKS = 40;
    private int missingRetry;
    /** Stations whose buffer changed; drained on the next tick. */
    private final Set<BlockPos> pendingStations = new LinkedHashSet<>();
    /** Stations holding a stack that found no room; retried on zone and rule changes and on a slow timer. */
    private final Set<BlockPos> blockedStations = new LinkedHashSet<>();
    private static final int BLOCKED_RETRY_TICKS = 20;
    private int blockedRetry;
    private final WarehouseIndex<BlockPos, ItemKey> index = new WarehouseIndex<>();
    private final ArrayDeque<BlockPos> dirtyQueue = new ArrayDeque<>();
    private final Set<BlockPos> dirtySet = new HashSet<>();
    private final List<BlockPos> pollOrder = new ArrayList<>();
    private boolean pollOrderStale = true;
    /** Set whenever membership or the controller count changed; the next tick pushes the lamp state. */
    private boolean lampsDirty;
    private int cursor;
    private ServerLevel level;
    private int controllers;
    /** The controller's position while exactly one is in the network. */
    private BlockPos controllerPos;
    /** Rules whose last order attempt found no producer. */
    private final Set<ItemKey> rulesWithoutProducer = new HashSet<>();
    private int ruleTimer;
    private int outboundTimer;
    private final ShuttleFleet fleet = new ShuttleFleet(this);
    private final OrderBook orders = new OrderBook(new OrderBook.Host() {
        @Override
        public List<Producer> producers() {
            return WarehouseNetwork.this.producers();
        }

        @Override
        public Map<ItemKey, Long> stock() {
            Map<ItemKey, Long> stock = new HashMap<>();
            for (Map.Entry<ItemKey, Long> entry : index.entries()) {
                stock.put(entry.getKey(), entry.getValue());
            }
            return stock;
        }

        @Override
        public OrderBook.Feed feed(BlockPos dock, ItemKey key, int amount) {
            Started started = planAndStart(dock, key, amount, 0);
            return new OrderBook.Feed(started.planned(), started.unservable(), started.refusal());
        }

        @Override
        public long gameTime() {
            return level == null ? 0 : level.getGameTime();
        }
    });

    public WarehouseNetwork(int id, NetworkSet<StorageNodeBlockEntity, WarehouseEvent, ?> owner) {
        super(id, owner);
    }

    // Membership

    @Override
    protected void onNodeAdded(NodePos pos, StorageNodeBlockEntity node) {
        if (!(node.getLevel() instanceof ServerLevel server)) {
            return;
        }
        level = server;
        BlockPos blockPos = Conversions.toBlockPos(pos);
        if (node.holdsStock()) {
            addSource(blockPos, true);
            addCandidatesAround(blockPos);
            rememberZone(blockPos, node);
            if (node instanceof CounterTerminalBlockEntity) {
                terminals.add(blockPos);
            }
            // A new storage block may give waiting stacks a place to go.
            zonesDirty = true;
        } else if (node.kind() == WarehouseConnectionRule.NodeKind.STATION) {
            // A stock neighbour may have listed this position as a candidate inventory; the buffer is never stock.
            dropSource(blockPos);
            stations.add(blockPos);
            pendingStations.add(blockPos);
        } else if (node instanceof ShuttleGarageBlockEntity garage) {
            dropSource(blockPos);
            garages.add(blockPos);
            fleet.adoptGarage(blockPos, garage.shuttleCount(), garage.isActive());
        } else {
            dropSource(blockPos);
            connectors.add(blockPos);
            if (node.kind() == WarehouseConnectionRule.NodeKind.ELEVATOR) {
                elevators.add(pos);
                shaftsDirty = true;
            }
        }
        if (node instanceof WarehouseControllerBlockEntity) {
            controllers++;
            controllerPos = blockPos;
        }
        lampsDirty = true;
        fleet.refreshHomes();
    }

    @Override
    protected void onNodeRemoved(NodePos pos, StorageNodeBlockEntity node) {
        BlockPos blockPos = Conversions.toBlockPos(pos);
        connectors.remove(blockPos);
        if (elevators.remove(pos)) {
            shaftsDirty = true;
        }
        stations.remove(blockPos);
        pendingStations.remove(blockPos);
        blockedStations.remove(blockPos);
        zoneOf.remove(blockPos);
        dropSource(blockPos);
        if (garages.remove(blockPos)) {
            fleet.releaseGarage(blockPos);
        }
        terminals.remove(blockPos);
        held.remove(blockPos);
        if (node instanceof WarehouseControllerBlockEntity) {
            controllers--;
            if (blockPos.equals(controllerPos)) {
                controllerPos = null;
            }
        }
        lampsDirty = true;
        rebuildCandidates();
        fleet.refreshHomes();
    }

    @Override
    protected void onAbsorb(ServusNetwork<StorageNodeBlockEntity, WarehouseEvent> other) {
        if (!(other instanceof WarehouseNetwork that)) {
            return;
        }
        if (level == null) {
            level = that.level;
        }
        controllers += that.controllers;
        if (controllerPos == null) {
            controllerPos = that.controllerPos;
        }
        connectors.addAll(that.connectors);
        elevators.addAll(that.elevators);
        shaftsDirty = true;
        stations.addAll(that.stations);
        pendingStations.addAll(that.stations);
        garages.addAll(that.garages);
        terminals.addAll(that.terminals);
        that.held.forEach((pos, loads) -> held.computeIfAbsent(pos, p -> new ArrayList<>()).addAll(loads));
        zoneOf.putAll(that.zoneOf);
        that.sources.forEach((pos, source) -> {
            if (!sources.containsKey(pos) || source.node()) {
                sources.put(pos, source);
                index.update(pos, that.index.snapshot(pos));
            }
        });
        pollOrderStale = true;
        lampsDirty = true;
        fleet.absorb(that.fleet);
    }

    /** A garage's shuttle count or pause state changed. */
    public void garageChanged(ShuttleGarageBlockEntity garage) {
        fleet.adoptGarage(garage.getBlockPos(), garage.shuttleCount(), garage.isActive());
    }

    /** Brings every shuttle of the garage inside; cargo drops at the garage and unloaded jobs are queued again. */
    public void recallGarage(BlockPos pos) {
        fleet.recall(pos);
    }

    /** Shuttles of one garage that are out on a job. */
    public int garageOut(BlockPos pos) {
        return fleet.outOf(pos);
    }

    /** One garage's line for its right click. */
    public Component garageSummary(BlockPos pos) {
        return Component.literal("Garage: " + fleet.garageSummary(pos));
    }

    /** The lift car of the shaft whose lowest segment is at that position, or null. */
    public io.github.dirnei.mc.servus.storage.shuttle.LiftCar lift(BlockPos bottom) {
        return fleet.lift(bottom);
    }

    /** The shuttles of one garage as clients draw them. */
    public List<io.github.dirnei.mc.servus.storage.shuttle.ShuttleView> shuttleViews(BlockPos garage) {
        return fleet.views(garage);
    }

    // Racks

    /** Whether the source is a Heavy Duty Rack, which only shuttles serve. */
    @Override
    public boolean isRack(BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof HeavyRackBlockEntity;
    }

    // Zones

    private void rememberZone(BlockPos pos, StorageNodeBlockEntity node) {
        ZoneDefinition definition = node.zoneDefinition();
        if (definition == null) {
            zoneOf.remove(pos);
        } else {
            zoneOf.put(pos, definition);
        }
    }

    /** Tag ids of an item, as the pure plan sees them. */
    public static Set<String> tagsOf(ItemKey key) {
        return key.stack(1).getTags().map(tag -> tag.location().toString()).collect(Collectors.toSet());
    }

    /** Sources with a non empty filter, as the pure plan sees them. */
    private Map<BlockPos, PutAwayPlan.Filter<ItemKey>> filtered() {
        Map<BlockPos, PutAwayPlan.Filter<ItemKey>> map = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, ZoneDefinition> entry : zoneOf.entrySet()) {
            if (!entry.getValue().filter().isEmpty() && sources.containsKey(entry.getKey())) {
                map.put(entry.getKey(), entry.getValue().filter().toPure());
            }
        }
        return map;
    }

    /** The zoned blocks that take this key, by exact item and by tag. */
    public PutAwayPlan.Tiers<BlockPos> tiers(ItemKey key) {
        return PutAwayPlan.tiers(key, filtered(), WarehouseNetwork::tagsOf);
    }

    /** The zone name of the first block that matched, or null when none did. */
    private String matchedZone(PutAwayPlan.Tiers<BlockPos> tiers) {
        for (BlockPos pos : tiers.exact()) {
            ZoneDefinition zone = zoneOf.get(pos);
            if (zone != null) {
                return zone.name();
            }
        }
        for (BlockPos pos : tiers.byTag()) {
            ZoneDefinition zone = zoneOf.get(pos);
            if (zone != null) {
                return zone.name();
            }
        }
        return null;
    }

    /** The target lists in order: exact matches, tag matches, then general storage. */
    private List<List<BlockPos>> targetTiers(ItemKey key, PutAwayPlan.Tiers<BlockPos> tiers) {
        List<List<BlockPos>> result = new ArrayList<>(3);
        result.add(order(tiers.exact(), key));
        result.add(order(tiers.byTag(), key));
        Set<BlockPos> general = new LinkedHashSet<>();
        for (Source source : sources.values()) {
            ZoneDefinition zone = zoneOf.get(source.pos());
            if (zone == null || zone.filter().isEmpty()) {
                general.add(source.pos());
            }
        }
        result.add(order(general, key));
        return result;
    }

    /** Orders candidates: holding the key, empty crates, other storage nodes, foreign inventories in use. */
    private List<BlockPos> order(Set<BlockPos> candidates, ItemKey key) {
        List<BlockPos> holding = new ArrayList<>();
        for (BlockPos pos : index.sourcesHolding(key)) {
            if (candidates.contains(pos) && sources.containsKey(pos)) {
                holding.add(pos);
            }
        }
        Set<BlockPos> holders = new HashSet<>(holding);
        List<BlockPos> emptyCrates = new ArrayList<>();
        List<BlockPos> otherNodes = new ArrayList<>();
        List<BlockPos> foreignInUse = new ArrayList<>();
        for (BlockPos pos : candidates) {
            Source source = sources.get(pos);
            if (source == null || holders.contains(pos)) {
                continue;
            }
            if (source.node()) {
                if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate && crate.isEmpty()) {
                    emptyCrates.add(pos);
                } else {
                    otherNodes.add(pos);
                }
            } else if (!index.isEmptySource(pos)) {
                foreignInUse.add(pos);
            }
        }
        List<BlockPos> result = new ArrayList<>(holding);
        result.addAll(emptyCrates);
        result.addAll(otherNodes);
        result.addAll(foreignInUse);
        return result;
    }

    /** Stock blocks per zone key, sorted by key. */
    public Map<String, Integer> zoneBlockCounts() {
        Map<String, Integer> counts = new TreeMap<>();
        for (ZoneDefinition zone : zoneOf.values()) {
            counts.merge(zone.key(), 1, Integer::sum);
        }
        return counts;
    }

    /** One zone as its blocks carry it: the union of their filters. */
    public record ZoneSummary(String key, String display, int blocks, List<String> items, List<String> tags) {
    }

    public ZoneSummary zoneSummary(String key) {
        String display = key;
        int blocks = 0;
        Set<String> items = new LinkedHashSet<>();
        Set<String> tags = new LinkedHashSet<>();
        for (ZoneDefinition zone : zoneOf.values()) {
            if (!zone.key().equals(key)) {
                continue;
            }
            blocks++;
            display = zone.name();
            items.addAll(zone.filter().itemNames());
            tags.addAll(zone.filter().tags());
        }
        return new ZoneSummary(key, display, blocks, List.copyOf(items), List.copyOf(tags));
    }

    /** Every zone of the warehouse, sorted by key. */
    public List<ZoneSummary> zoneSummaries() {
        List<ZoneSummary> list = new ArrayList<>();
        for (String key : zoneBlockCounts().keySet()) {
            list.add(zoneSummary(key));
        }
        return list;
    }

    // Shuttle host

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public Set<NodePos> travelNeighbours(NodePos node) {
        Set<NodePos> result = new HashSet<>();
        for (NodePos neighbour : owner().graph().neighbors(node)) {
            if (isTravelNode(neighbour)) {
                result.add(neighbour);
            }
        }
        return result;
    }

    @Override
    public boolean isTravelNode(NodePos node) {
        return connectors.contains(Conversions.toBlockPos(node)) && members().contains(node);
    }

    /**
     * A rack is reached only through the rails in front of the bays of its
     * section; any other block through every travel node touching it.
     */
    @Override
    public Set<NodePos> dockRails(NodePos storage) {
        Set<NodePos> result = new HashSet<>();
        BlockPos pos = Conversions.toBlockPos(storage);
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof HeavyRackBlock) {
            for (BlockPos bay : HeavyRackBlock.section(level, pos)) {
                NodePos front = Conversions.toNodePos(bay.relative(level.getBlockState(bay).getValue(HeavyRackBlock.FACING)));
                if (isTravelNode(front)) {
                    result.add(front);
                }
            }
            return result;
        }
        for (Side side : Side.ALL) {
            NodePos beside = storage.relative(side);
            if (isTravelNode(beside)) {
                result.add(beside);
            }
        }
        return result;
    }

    @Override
    public List<BlockPos> sourcesHolding(ItemKey key) {
        return index.sourcesHolding(key);
    }

    @Override
    public ItemStack extractFrom(BlockPos pos, ItemKey key, int amount) {
        Source source = sources.get(pos);
        IItemHandler handler = source == null ? null : source.cache().getCapability();
        if (handler == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        int wanted = Math.min(amount, key.stack(1).getMaxStackSize());
        for (int slot = 0; slot < handler.getSlots() && wanted > 0; slot++) {
            if (!key.matches(handler.getStackInSlot(slot))) {
                continue;
            }
            ItemStack taken = handler.extractItem(slot, wanted, false);
            if (taken.isEmpty()) {
                continue;
            }
            if (result.isEmpty()) {
                result = taken;
            } else {
                result.grow(taken.getCount());
            }
            wanted -= taken.getCount();
        }
        if (!result.isEmpty()) {
            markDirty(pos);
        }
        return result;
    }

    @Override
    public double shuttleSpeed() {
        return StorageStatsManager.INSTANCE.shuttleSpeed();
    }

    @Override
    public double liftSpeed() {
        return StorageStatsManager.INSTANCE.liftSpeed();
    }

    @Override
    public Shaft shaftOf(NodePos node) {
        return shaftFloors.get(node);
    }

    // Terminal requests

    /** Instant loads that did not fit into a terminal's output slots yet, per terminal. */
    private final Map<BlockPos, List<HeldLoad>> held = new HashMap<>();

    private record HeldLoad(PickJob job, ItemStack stack) {
    }

    /** Whether some garage shuttle can drive to a rail beside the position. */
    public boolean canReach(BlockPos pos) {
        return fleet.canReach(pos);
    }

    /**
     * Plans and starts a request at a terminal.
     *
     * @return an empty string when the request was made, else the refusal key:
     *         {@code offline}, {@code needs_shuttle}, {@code no_rail} or {@code out_of_stock}
     */
    public String request(CounterTerminalBlockEntity terminal, ItemKey key, int amount) {
        if (status() != Status.ONLINE) {
            return "offline";
        }
        BlockPos destination = terminal.getBlockPos();
        boolean reachable = fleet.canReach(destination);
        RequestPlan.Plan<BlockPos> plan = RequestPlan.plan(amount, key.stack(1).getMaxStackSize(), reachable, stockOf(key));
        if (plan.jobs().isEmpty() && plan.needsShuttle()) {
            return fleet.hasShuttles() && !reachable ? "no_rail" : "needs_shuttle";
        }
        CounterTerminalBlockEntity.Request request = terminal.newRequest(key, amount, plan.unservable());
        startJobs(plan, request.id, key, destination);
        if (plan.unservable() > 0) {
            OrderBook.Created created = orders.create(key, plan.unservable(), request.id);
            if (!created.made() && plan.jobs().isEmpty()) {
                terminal.removeRequest(request.id);
                return "out_of_stock";
            }
        }
        return "";
    }

    /** Delivers items into an outbound station's buffer, planned and refused like a request. */
    public String feedStation(OutboundStationBlockEntity station, ItemKey key, int amount) {
        return planAndStart(station.getBlockPos(), key, amount, 0).refusal();
    }

    /** The work orders of this warehouse. */
    public OrderBook orders() {
        return orders;
    }

    // Outbound stations

    /** Tops every outbound station's buffer up to its keep amounts and records what could not be filled. */
    private void tickOutbound() {
        if (status() != Status.ONLINE || level == null) {
            return;
        }
        for (BlockPos pos : new ArrayList<>(stations)) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof OutboundStationBlockEntity station)) {
                continue;
            }
            ItemStack shortStack = ItemStack.EMPTY;
            String shortReason = "";
            boolean pending = false;
            for (int slot = 0; slot < station.keep().getSlots(); slot++) {
                ItemStack wanted = station.keep().getStackInSlot(slot);
                if (wanted.isEmpty()) {
                    continue;
                }
                ItemKey key = ItemKey.of(wanted);
                int onTheWay = pendingTo(pos, key);
                pending |= onTheWay > 0;
                int have = station.bufferCount(key) + onTheWay;
                if (have >= wanted.getCount()) {
                    continue;
                }
                int missing = wanted.getCount() - have;
                Started started = planAndStart(pos, key, missing, 0);
                pending |= started.planned() > 0;
                if (started.planned() < missing && shortStack.isEmpty()) {
                    shortStack = key.stack(1);
                    shortReason = started.refusal().isEmpty() ? "out_of_stock" : started.refusal();
                }
            }
            station.setRestockState(shortStack, shortReason, pending);
        }
    }

    /** Items of the key queued, riding or held for a destination. */
    private int pendingTo(BlockPos destination, ItemKey key) {
        return fleet.queuedAmount(destination, key) + fleet.ridingAmount(destination, key) + heldAmount(destination, key);
    }

    private int heldAmount(BlockPos destination, ItemKey key) {
        int count = 0;
        for (HeldLoad load : held.getOrDefault(destination, List.of())) {
            if (load.job().key().equals(key)) {
                count += load.stack().getCount();
            }
        }
        return count;
    }

    // Order board

    public boolean cancelOrder(int orderId) {
        return orders.cancel(orderId);
    }

    /** Every dock with its machine and what it works on. */
    public List<MachineRow> machineRows() {
        List<MachineRow> rows = new ArrayList<>();
        for (Producer producer : producers()) {
            ItemStack working = ItemStack.EMPTY;
            String state;
            if (!producer.producer()) {
                state = "no_producer";
            } else if (producer.recipes().isEmpty()) {
                state = "no_recipes";
            } else if (producer.active().isPresent()) {
                state = "working";
                for (ProductionRecipe recipe : producer.recipes()) {
                    if (recipe.id().equals(producer.active().get()) && !recipe.outputs().isEmpty()) {
                        working = recipe.outputs().get(0).copyWithCount(1);
                    }
                }
            } else {
                state = "idle";
            }
            rows.add(new MachineRow(producer.station(), producer.machineName().getString(), working, state));
        }
        return rows;
    }

    /** Every work order with its steps, machine names from the docks. */
    public List<OrderRow> orderRows() {
        Map<BlockPos, String> names = new HashMap<>();
        for (Producer producer : producers()) {
            names.put(producer.station(), producer.machineName().getString());
        }
        List<OrderRow> rows = new ArrayList<>();
        for (WorkOrder order : orders.orders()) {
            List<StepRow> steps = new ArrayList<>();
            for (OrderStep step : order.steps()) {
                steps.add(new StepRow(step.station(), names.getOrDefault(step.station(), "?"), step.output().copyWithCount(1), step.batches(),
                        step.state().name().toLowerCase(java.util.Locale.ROOT), step.reason()));
            }
            rows.add(new OrderRow(order.id(), order.key().stack(1), order.amount(), order.state().name().toLowerCase(java.util.Locale.ROOT),
                    order.reason(), order.requestId(), steps));
        }
        return rows;
    }

    // Stock rules

    /** The controller's stock rules, present while a controller is in the network and loaded. */
    public Optional<StockRules> stockRules() {
        if (controllerPos == null || level == null || !level.isLoaded(controllerPos)) {
            return Optional.empty();
        }
        return level.getBlockEntity(controllerPos) instanceof WarehouseControllerBlockEntity controller
                ? Optional.of(controller.rules()) : Optional.empty();
    }

    public void setStockRule(ItemKey key, int min, int max) {
        stockRules().ifPresent(rules -> {
            rules.set(new StockRule(key, min, max));
            rulesWithoutProducer.remove(key);
        });
        ruleTimer = StorageStatsManager.INSTANCE.stockRuleInterval();
    }

    public boolean removeStockRule(ItemKey key) {
        rulesWithoutProducer.remove(key);
        return stockRules().map(rules -> rules.remove(key)).orElse(false);
    }

    /** Orders for every rule whose stock plus open orders fell below its minimum. */
    private void tickStockRules() {
        if (status() != Status.ONLINE) {
            return;
        }
        Optional<StockRules> rules = stockRules();
        if (rules.isEmpty()) {
            return;
        }
        for (StockRule rule : rules.get().all()) {
            long have = index.count(rule.key()) + orders.openAmountFor(rule.key());
            if (have >= rule.min()) {
                rulesWithoutProducer.remove(rule.key());
                continue;
            }
            int wanted = (int) Math.min(Integer.MAX_VALUE, rule.max() - have);
            OrderBook.Created created = orders.create(rule.key(), wanted, OrderBook.RULE_ORIGIN);
            if (created.made()) {
                rulesWithoutProducer.remove(rule.key());
            } else if (created.refusal().equals("no_producer")) {
                rulesWithoutProducer.add(rule.key());
            }
        }
    }

    /** The state of a rule: ordering, no producer, or ok. */
    public String ruleState(StockRule rule) {
        if (orders.openAmountFor(rule.key()) > 0) {
            return "ordering";
        }
        return rulesWithoutProducer.contains(rule.key()) ? "no_producer" : "ok";
    }

    public List<RuleRow> ruleRows() {
        List<RuleRow> rows = new ArrayList<>();
        stockRules().ifPresent(rules -> {
            for (StockRule rule : rules.all()) {
                rows.add(new RuleRow(rule.key().stack(1), rule.min(), rule.max(), ruleState(rule)));
            }
        });
        return rows;
    }

    /** Creates a work order by hand; returns the refusal key or an empty string. */
    public String order(ItemKey key, int amount) {
        if (status() != Status.ONLINE) {
            return "offline";
        }
        return orders.create(key, amount, 0).refusal();
    }

    private List<RequestPlan.Stock<BlockPos>> stockOf(ItemKey key) {
        List<RequestPlan.Stock<BlockPos>> stock = new ArrayList<>();
        for (BlockPos pos : index.sourcesHolding(key)) {
            Integer count = index.snapshot(pos).get(key);
            if (count == null || count <= 0 || !sources.containsKey(pos)) {
                continue;
            }
            stock.add(new RequestPlan.Stock<>(pos, count, isRack(pos), fleet.canReach(pos)));
        }
        return stock;
    }

    /** What a plan and start did: how many are on their way, how many the stock lacked, or why nothing happened. */
    public record Started(String refusal, int planned, int unservable) {
    }

    private Started planAndStart(BlockPos destination, ItemKey key, int amount, int requestId) {
        if (status() != Status.ONLINE) {
            return new Started("offline", 0, amount);
        }
        boolean reachable = fleet.canReach(destination);
        RequestPlan.Plan<BlockPos> plan = RequestPlan.plan(amount, key.stack(1).getMaxStackSize(), reachable, stockOf(key));
        if (plan.jobs().isEmpty()) {
            if (plan.needsShuttle()) {
                return new Started(fleet.hasShuttles() && !reachable ? "no_rail" : "needs_shuttle", 0, amount);
            }
            return new Started("out_of_stock", 0, amount);
        }
        startJobs(plan, requestId, key, destination);
        return new Started("", plan.planned(), plan.unservable());
    }

    private void startJobs(RequestPlan.Plan<BlockPos> plan, int requestId, ItemKey key, BlockPos destination) {
        for (RequestPlan.Job<BlockPos> job : plan.jobs()) {
            PickJob pick = new PickJob(requestId, key, job.count(), job.source(), destination);
            if (job.byShuttle()) {
                fleet.requestPick(pick);
            } else {
                ItemStack taken = extractFrom(job.source(), key, job.count());
                pickLoaded(pick, taken.getCount());
                deliverInstant(pick, taken);
            }
        }
    }

    /** An outbound station and what the block it faces can make. */
    public record Producer(BlockPos station, BlockPos machine, Component machineName, boolean producer, List<ProductionRecipe> recipes,
                           Optional<ResourceLocation> active) {
    }

    /** Every outbound station of the warehouse with its producer's recipes; stations without a producer list none. */
    public List<Producer> producers() {
        List<Producer> list = new ArrayList<>();
        if (level == null) {
            return list;
        }
        for (BlockPos pos : stations) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof OutboundStationBlockEntity station) {
                ProductionProvider provider = station.producer();
                BlockPos machine = station.machinePos();
                Component name = level.getBlockState(machine).getBlock().getName();
                list.add(new Producer(pos, machine, name, provider != null, provider == null ? List.of() : provider.recipes(),
                        provider == null ? Optional.empty() : provider.active()));
            }
        }
        return list;
    }

    /** Plans the missing part of every idle request again; stock may have arrived meanwhile. */
    private void retryMissing() {
        if (status() != Status.ONLINE) {
            return;
        }
        for (BlockPos pos : terminals) {
            CounterTerminalBlockEntity terminal = terminalAt(pos);
            if (terminal == null) {
                continue;
            }
            for (CounterTerminalBlockEntity.Request request : new ArrayList<>(terminal.requests())) {
                if (request.missing <= 0 || fleet.queuedFor(request.id) > 0 || fleet.ridingFor(request.id) > 0 || heldFor(pos, request.id) > 0) {
                    continue;
                }
                // While an order produces for it, only pick up what has actually arrived in storage.

                RequestPlan.Plan<BlockPos> plan = RequestPlan.plan(request.missing, request.key.stack(1).getMaxStackSize(),
                        fleet.canReach(pos), stockOf(request.key));
                request.missing -= plan.planned();
                startJobs(plan, request.id, request.key, pos);
            }
        }
    }

    /** Cancels a request: queued loads are dropped, loads on a shuttle still arrive in the output slots. */
    public void cancelRequest(CounterTerminalBlockEntity terminal, int requestId) {
        fleet.removeQueued(requestId);
        terminal.removeRequest(requestId);
        orders.cancelForRequest(requestId);
        // Held and riding loads keep trying to land: the items are already out of the shelves.
    }

    /** The open requests of a terminal as the GUI shows them, after dropping finished ones. */
    public List<RequestRow> requestRows(CounterTerminalBlockEntity terminal) {
        BlockPos terminalPos = terminal.getBlockPos();
        terminal.prune(request -> fleet.queuedFor(request.id) > 0 || fleet.ridingFor(request.id) > 0 || heldFor(terminalPos, request.id) > 0
                || orders.hasOpenOrderFor(request.id));
        List<RequestRow> rows = new ArrayList<>();
        for (CounterTerminalBlockEntity.Request request : terminal.requests()) {
            int queued = fleet.queuedFor(request.id);
            int riding = fleet.ridingFor(request.id);
            int heldCount = heldFor(terminalPos, request.id);
            CounterTerminalBlockEntity.RequestState state;
            if (request.waitingRoom || heldCount > 0) {
                state = CounterTerminalBlockEntity.RequestState.WAITING_ROOM;
            } else if (riding > 0) {
                state = CounterTerminalBlockEntity.RequestState.PICKING;
            } else if (queued > 0) {
                state = CounterTerminalBlockEntity.RequestState.WAITING_SHUTTLE;
            } else if (orders.hasOpenOrderFor(request.id)) {
                state = CounterTerminalBlockEntity.RequestState.PRODUCING;
            } else {
                state = CounterTerminalBlockEntity.RequestState.MISSING;
            }
            rows.add(new RequestRow(request.id, request.key.stack(1), request.requested, request.delivered, request.missing, state.name()));
        }
        return rows;
    }

    private int heldFor(BlockPos terminal, int requestId) {
        int count = 0;
        for (HeldLoad load : held.getOrDefault(terminal, List.of())) {
            if (load.job().requestId() == requestId) {
                count++;
            }
        }
        return count;
    }

    private CounterTerminalBlockEntity terminalAt(BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof CounterTerminalBlockEntity terminal ? terminal : null;
    }

    @Override
    public void pickLoaded(PickJob job, int loaded) {
        CounterTerminalBlockEntity terminal = terminalAt(job.terminal());
        CounterTerminalBlockEntity.Request request = terminal == null ? null : terminal.request(job.requestId());
        if (request != null && loaded < job.amount()) {
            request.missing += job.amount() - loaded;
        }
    }

    @Override
    public void pickDropped(PickJob job, int count) {
        CounterTerminalBlockEntity terminal = terminalAt(job.terminal());
        CounterTerminalBlockEntity.Request request = terminal == null ? null : terminal.request(job.requestId());
        if (request != null) {
            request.missing += count;
        }
    }

    @Override
    public ItemStack deliverTo(PickJob job, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        BlockEntity destination = level == null ? null : level.getBlockEntity(job.terminal());
        if (!(destination instanceof DeliveryTarget target)) {
            Block.popResource(level, job.terminal(), stack);
            return ItemStack.EMPTY;
        }
        int before = stack.getCount();
        ItemStack left = ItemHandlerHelper.insertItem(target.deliveries(), stack, false);
        if (destination instanceof CounterTerminalBlockEntity terminal) {
            CounterTerminalBlockEntity.Request request = terminal.request(job.requestId());
            if (request != null) {
                request.delivered += before - left.getCount();
                request.waitingRoom = !left.isEmpty();
            }
        }
        return left;
    }

    @Override
    public void deliverInstant(PickJob job, ItemStack stack) {
        ItemStack left = deliverTo(job, stack);
        if (!left.isEmpty()) {
            held.computeIfAbsent(job.terminal(), pos -> new ArrayList<>()).add(new HeldLoad(job, left));
        }
    }

    /** Retries the held loads of a terminal after its output slots changed. */
    private void retryHeld(BlockPos terminalPos) {
        List<HeldLoad> loads = held.get(terminalPos);
        if (loads == null) {
            return;
        }
        List<HeldLoad> remaining = new ArrayList<>();
        for (HeldLoad load : loads) {
            ItemStack left = deliverTo(load.job(), load.stack());
            if (!left.isEmpty()) {
                remaining.add(new HeldLoad(load.job(), left));
            }
        }
        if (remaining.isEmpty()) {
            held.remove(terminalPos);
        } else {
            held.put(terminalPos, remaining);
        }
    }


    @Override
    protected void onDisposed() {
        sources.clear();
        connectors.clear();
        elevators.clear();
        shaftFloors = Map.of();
        stations.clear();
        garages.clear();
        terminals.clear();
        held.clear();
        pendingStations.clear();
        blockedStations.clear();
        zoneOf.clear();
        dirtyQueue.clear();
        dirtySet.clear();
        pollOrder.clear();
        controllers = 0;
    }

    private void addSource(BlockPos pos, boolean node) {
        Source existing = sources.get(pos);
        if (existing != null && existing.node() == node) {
            markDirty(pos);
            return;
        }
        sources.put(pos, new Source(pos, node, BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, pos, null)));
        pollOrderStale = true;
        markDirty(pos);
    }

    private void dropSource(BlockPos pos) {
        if (sources.remove(pos) != null) {
            index.remove(pos);
            dirtySet.remove(pos);
            pollOrderStale = true;
        }
    }

    private void addCandidatesAround(BlockPos nodePos) {
        if (connectors.contains(nodePos)) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = nodePos.relative(direction);
            if (connectors.contains(neighbour) || stations.contains(neighbour) || garages.contains(neighbour)) {
                continue;
            }
            if (!sources.containsKey(neighbour)) {
                addSource(neighbour, false);
            } else {
                markDirty(neighbour);
            }
        }
    }

    /** Candidates are the neighbours of the remaining stock holding members that are not members. */
    private void rebuildCandidates() {
        Set<NodePos> memberPositions = new HashSet<>(members());
        Set<NodePos> connectorPositions = new HashSet<>();
        for (BlockPos connector : connectors) {
            connectorPositions.add(Conversions.toNodePos(connector));
        }
        for (BlockPos station : stations) {
            connectorPositions.add(Conversions.toNodePos(station));
        }
        for (BlockPos garage : garages) {
            connectorPositions.add(Conversions.toNodePos(garage));
        }
        Set<BlockPos> wanted = new LinkedHashSet<>();
        for (NodePos pos : CandidateNeighbours.compute(memberPositions, connectorPositions)) {
            wanted.add(Conversions.toBlockPos(pos));
        }
        for (BlockPos pos : new ArrayList<>(sources.keySet())) {
            Source source = sources.get(pos);
            if (!source.node() && !wanted.contains(pos)) {
                dropSource(pos);
            }
        }
        for (BlockPos pos : wanted) {
            if (!sources.containsKey(pos)) {
                addSource(pos, false);
            }
        }
    }

    // Events and polling

    @Override
    protected void handle(WarehouseEvent event) {
        if (event instanceof WarehouseEvent.Dirty dirty) {
            if (sources.containsKey(dirty.pos())) {
                markDirty(dirty.pos());
            }
        } else if (event instanceof WarehouseEvent.NeighborsChanged changed) {
            if (sources.containsKey(changed.pos())) {
                addCandidatesAround(changed.pos());
            }
        } else if (event instanceof WarehouseEvent.OutputChanged output) {
            retryHeld(output.pos());
        } else if (event instanceof WarehouseEvent.Intake intake) {
            if (stations.contains(intake.pos())) {
                pendingStations.add(intake.pos());
            }
        } else if (event instanceof WarehouseEvent.ZoneChanged zoned) {
            if (level != null && level.getBlockEntity(zoned.pos()) instanceof StorageNodeBlockEntity node && node.holdsStock()) {
                rememberZone(zoned.pos(), node);
                zonesDirty = true;
            }
        }
    }

    private void markDirty(BlockPos pos) {
        if (dirtySet.add(pos)) {
            dirtyQueue.add(pos);
        }
    }

    @Override
    protected void onTick() {
        if (lampsDirty && level != null) {
            pushLamps();
        }
        if (level != null) {
            if (shaftsDirty) {
                shaftsDirty = false;
                List<Shaft> shafts = Shafts.group(elevators);
                shaftFloors = Shafts.floorsOf(shafts);
                fleet.refreshShafts(shafts);
            }
            fleet.tick();
            tickStations();
            orders.tick();
            if (++outboundTimer >= StorageStatsManager.INSTANCE.outboundCheckInterval()) {
                outboundTimer = 0;
                tickOutbound();
            }
            if (++ruleTimer >= StorageStatsManager.INSTANCE.stockRuleInterval()) {
                ruleTimer = 0;
                tickStockRules();
            }
            if (!terminals.isEmpty() && ++missingRetry >= MISSING_RETRY_TICKS) {
                missingRetry = 0;
                retryMissing();
            }
        }
        if (level == null || sources.isEmpty()) {
            return;
        }
        int budget = StorageStatsManager.INSTANCE.sourcesPerTick();
        while (budget > 0 && !dirtyQueue.isEmpty()) {
            BlockPos pos = dirtyQueue.poll();
            dirtySet.remove(pos);
            if (sources.containsKey(pos)) {
                poll(pos);
                budget--;
            }
        }
        if (pollOrderStale) {
            pollOrder.clear();
            pollOrder.addAll(sources.keySet());
            pollOrderStale = false;
        }
        for (int i = 0; i < budget && !pollOrder.isEmpty(); i++) {
            cursor = (cursor + 1) % pollOrder.size();
            BlockPos pos = pollOrder.get(cursor);
            if (sources.containsKey(pos)) {
                poll(pos);
            }
        }
    }

    // Inbound stations

    private void tickStations() {
        if (stations.isEmpty()) {
            return;
        }
        if (zonesDirty) {
            zonesDirty = false;
            pendingStations.addAll(blockedStations);
        }
        if (!blockedStations.isEmpty() && ++blockedRetry >= BLOCKED_RETRY_TICKS) {
            blockedRetry = 0;
            pendingStations.addAll(blockedStations);
        }
        if (pendingStations.isEmpty() || status() != Status.ONLINE) {
            return;
        }
        for (BlockPos pos : new ArrayList<>(pendingStations)) {
            pendingStations.remove(pos);
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof InboundStationBlockEntity station) {
                drain(pos, station);
            }
        }
    }

    /** Moves up to {@code put_away_per_tick} unclaimed stacks out of the station's buffer. */
    private void drain(BlockPos pos, InboundStationBlockEntity station) {
        int budget = StorageStatsManager.INSTANCE.putAwayPerTick();
        ItemKey blockedKey = null;
        String blockedZone = null;
        boolean blockedByRack = false;
        boolean more = false;
        for (int slot = 0; slot < station.buffer().getSlots(); slot++) {
            ItemStack stack = station.buffer().getStackInSlot(slot);
            if (stack.isEmpty() || station.isClaimed(slot)) {
                continue;
            }
            if (budget <= 0) {
                more = true;
                break;
            }
            ItemKey key = ItemKey.of(stack);
            PutAwayPlan.Tiers<BlockPos> tiers = tiers(key);
            Optional<BlockPos> target = chooseTarget(tiers, key, stack, rack -> fleet.canCarry(pos, rack));
            if (target.isEmpty()) {
                if (blockedKey == null) {
                    blockedKey = key;
                    blockedZone = matchedZone(tiers);
                    blockedByRack = rackHasRoom(tiers, key, stack);
                }
                continue;
            }
            budget--;
            orders.collected(pos, key, stack.getCount());
            if (fleet.canCarry(pos, target.get())) {
                station.claim(slot);
                fleet.requestPutAway(new PutAwayJob(pos, slot, key, stack.getCount(), target.get()));
            } else {
                ItemStack taken = station.buffer().extractItem(slot, stack.getCount(), false);
                ItemStack left = insertAt(target.get(), taken);
                returnToStation(pos, left);
            }
        }
        if (more) {
            pendingStations.add(pos);
        }
        if (blockedKey != null) {
            blockedStations.add(pos);
            PutAwayPlan.Reason reason = blockedByRack ? PutAwayPlan.Reason.NO_SHUTTLE
                    : hasStorage(blockedKey) ? PutAwayPlan.Reason.NO_ROOM : PutAwayPlan.Reason.NO_STORAGE;
            station.setBlocked(reason, blockedKey, blockedZone);
        } else {
            blockedStations.remove(pos);
            station.clearBlocked();
        }
    }

    /** Whether any source the key could go to has an inventory at all; the controller alone has none. */
    private boolean hasStorage(ItemKey key) {
        for (List<BlockPos> tier : targetTiers(key, tiers(key))) {
            for (BlockPos pos : tier) {
                Source source = sources.get(pos);
                if (source != null && source.cache().getCapability() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The first block in tier order that takes at least one item of the stack.
     * Racks count only when {@code rackAllowed} says a shuttle can carry to them.
     */
    public Optional<BlockPos> chooseTarget(PutAwayPlan.Tiers<BlockPos> tiers, ItemKey key, ItemStack stack, Predicate<BlockPos> rackAllowed) {
        for (List<BlockPos> tier : targetTiers(key, tiers)) {
            for (BlockPos pos : tier) {
                if (isRack(pos) && !rackAllowed.test(pos)) {
                    continue;
                }
                if (hasRoom(pos, stack)) {
                    return Optional.of(pos);
                }
            }
        }
        return Optional.empty();
    }

    private boolean hasRoom(BlockPos pos, ItemStack stack) {
        Source source = sources.get(pos);
        IItemHandler handler = source == null ? null : source.cache().getCapability();
        return handler != null && ItemHandlerHelper.insertItem(handler, stack, true).getCount() < stack.getCount();
    }

    /** Whether some rack in the tiers has room for the stack. */
    private boolean rackHasRoom(PutAwayPlan.Tiers<BlockPos> tiers, ItemKey key, ItemStack stack) {
        for (List<BlockPos> tier : targetTiers(key, tiers)) {
            for (BlockPos pos : tier) {
                if (isRack(pos) && hasRoom(pos, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Inserts into exactly that source; returns what did not fit. */
    @Override
    public ItemStack insertAt(BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        Source source = sources.get(pos);
        IItemHandler handler = source == null ? null : source.cache().getCapability();
        if (handler == null) {
            return stack;
        }
        ItemStack left = ItemHandlerHelper.insertItem(handler, stack, false);
        if (left.getCount() != stack.getCount()) {
            markDirty(pos);
        }
        return left;
    }

    /** Puts a stack back into a station's buffer, dropping what does not fit there. */
    @Override
    public void returnToStation(BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack left = stack;
        if (level.getBlockEntity(pos) instanceof InboundStationBlockEntity station) {
            left = ItemHandlerHelper.insertItem(station.buffer(), stack, false);
        }
        if (!left.isEmpty()) {
            Block.popResource(level, pos, left);
        }
    }

    @Override
    public void releaseClaim(BlockPos station, int slot) {
        if (level != null && level.getBlockEntity(station) instanceof InboundStationBlockEntity be) {
            be.release(slot);
            pendingStations.add(station);
        }
    }

    /** Takes up to amount of the key out of a station's buffer and frees the slot's claim. */
    @Override
    public ItemStack takeFromStation(BlockPos pos, int slot, ItemKey key, int amount) {
        if (!(level.getBlockEntity(pos) instanceof InboundStationBlockEntity station)) {
            return ItemStack.EMPTY;
        }
        station.release(slot);
        ItemStack result = ItemStack.EMPTY;
        int wanted = amount;
        for (int i = 0; i < station.buffer().getSlots() && wanted > 0; i++) {
            ItemStack inSlot = station.buffer().getStackInSlot(i);
            if (!key.matches(inSlot) || (i != slot && station.isClaimed(i))) {
                continue;
            }
            ItemStack taken = station.buffer().extractItem(i, wanted, false);
            if (taken.isEmpty()) {
                continue;
            }
            if (result.isEmpty()) {
                result = taken;
            } else {
                result.grow(taken.getCount());
            }
            wanted -= taken.getCount();
        }
        return result;
    }

    /** Writes the online state into every loaded stock holding member whose lamp is wrong. */
    private void pushLamps() {
        lampsDirty = false;
        boolean online = status() == Status.ONLINE;
        for (NodePos member : members()) {
            BlockPos pos = Conversions.toBlockPos(member);
            if (connectors.contains(pos) || !level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(StorageNodeBlocks.ONLINE) && state.getValue(StorageNodeBlocks.ONLINE) != online) {
                level.setBlock(pos, state.setValue(StorageNodeBlocks.ONLINE, online), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private void poll(BlockPos pos) {
        Source source = sources.get(pos);
        IItemHandler handler = source.cache().getCapability();
        long before = total(index.snapshot(pos));
        if (handler == null) {
            if (source.node()) {
                index.remove(pos);
            } else {
                dropSource(pos);
            }
            return;
        }
        Map<ItemKey, Integer> snapshot = new HashMap<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                snapshot.merge(ItemKey.of(stack), stack.getCount(), Integer::sum);
            }
        }
        index.update(pos, snapshot);
        if (!blockedStations.isEmpty() && total(snapshot) < before) {
            // Room appeared somewhere; waiting stacks may fit now.
            zonesDirty = true;
        }
    }

    private static long total(Map<ItemKey, Integer> snapshot) {
        long sum = 0;
        for (int count : snapshot.values()) {
            sum += count;
        }
        return sum;
    }

    // Queries

    public Status status() {
        if (controllers == 1) {
            return Status.ONLINE;
        }
        return controllers == 0 ? Status.NO_CONTROLLER : Status.MULTIPLE_CONTROLLERS;
    }

    public WarehouseIndex<BlockPos, ItemKey> index() {
        return index;
    }

    public int sourceCount() {
        return sources.size();
    }

    public Component summary() {
        String state = switch (status()) {
            case ONLINE -> "online";
            case NO_CONTROLLER -> "offline, no controller";
            case MULTIPLE_CONTROLLERS -> "offline, more than one controller";
        };
        return Component.literal("Warehouse #" + id() + ": " + state + ", " + size() + " nodes, " + connectors.size() + " connectors, " + sources.size()
                + " sources, " + stations.size() + " stations, " + garages.size() + " garages, " + index.typeCount() + " item types, " + index.totalItems() + " items, " + fleet.summary());
    }

    // Operations

    /** Pulls up to {@code amount} (at most one stack) of the key out of the warehouse. */
    public ItemStack extract(ItemKey key, int amount) {
        ItemStack result = ItemStack.EMPTY;
        int wanted = Math.min(amount, key.stack(1).getMaxStackSize());
        for (BlockPos pos : index.sourcesHolding(key)) {
            if (isRack(pos)) {
                continue;
            }
            Source source = sources.get(pos);
            IItemHandler handler = source == null ? null : source.cache().getCapability();
            if (handler == null) {
                continue;
            }
            for (int slot = 0; slot < handler.getSlots() && wanted > 0; slot++) {
                if (!key.matches(handler.getStackInSlot(slot))) {
                    continue;
                }
                ItemStack taken = handler.extractItem(slot, wanted, false);
                if (taken.isEmpty()) {
                    continue;
                }
                if (result.isEmpty()) {
                    result = taken;
                } else {
                    result.grow(taken.getCount());
                }
                wanted -= taken.getCount();
            }
            markDirty(pos);
            if (wanted <= 0) {
                break;
            }
        }
        return result;
    }

    /**
     * Puts a stack into the warehouse instantly by the put-away routing: zoned
     * blocks listing the exact item, then zoned blocks matching by tag, then
     * general storage; within a tier sources already holding it, then empty
     * crates, then other storage nodes, then foreign inventories in use.
     * Racks are skipped: only shuttles fill them.
     *
     * @return what did not fit
     */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        ItemKey key = ItemKey.of(stack);
        ItemStack remaining = stack.copy();
        for (List<BlockPos> tier : targetTiers(key, tiers(key))) {
            for (BlockPos pos : tier) {
                if (remaining.isEmpty()) {
                    break;
                }
                if (!isRack(pos)) {
                    remaining = insertInto(List.of(pos), remaining);
                }
            }
            if (remaining.isEmpty()) {
                break;
            }
        }
        return remaining;
    }

    private ItemStack insertInto(List<BlockPos> positions, ItemStack remaining) {
        for (BlockPos pos : positions) {
            if (remaining.isEmpty()) {
                break;
            }
            Source source = sources.get(pos);
            IItemHandler handler = source == null ? null : source.cache().getCapability();
            if (handler == null) {
                continue;
            }
            int before = remaining.getCount();
            remaining = ItemHandlerHelper.insertItem(handler, remaining, false);
            if (remaining.getCount() != before) {
                markDirty(pos);
            }
        }
        return remaining;
    }
}
