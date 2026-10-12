package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.shuttle.RequestPlan;
import io.github.dirnei.mc.servus.storage.site.SiteChunks;
import java.util.function.Predicate;
import io.github.dirnei.mc.servus.storage.index.WarehouseIndex;
import io.github.dirnei.mc.servus.storage.shuttle.PickJob;
import io.github.dirnei.mc.servus.storage.shuttle.PutAwayJob;
import io.github.dirnei.mc.servus.storage.shuttle.FertilizeJob;
import io.github.dirnei.mc.servus.storage.shuttle.FieldJob;
import io.github.dirnei.mc.servus.storage.shuttle.module.JobNeeds;
import io.github.dirnei.mc.servus.storage.field.FieldDue;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldHeads;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleFleet;
import io.github.dirnei.mc.servus.storage.shuttle.Shafts;
import io.github.dirnei.mc.servus.storage.shuttle.Shafts.Shaft;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.PackingStationChoice;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.storage.shuttle.RailPaths;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.storage.order.StockRule;
import io.github.dirnei.mc.servus.storage.order.StockRules;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;

/**
 * One connected set of storage blocks. Keeps an index of everything its own
 * racks hold, refreshed in batches; inventories touching a member are never
 * indexed. Every pick and put-away runs through racks and shuttles.
 */
public final class WarehouseNetwork extends ServusNetwork<StorageNodeBlockEntity, WarehouseEvent> implements ShuttleFleet.Host {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "warehouse");

    public static final NetworkType<StorageNodeBlockEntity, WarehouseEvent, WarehouseNetwork> TYPE =
            new NetworkType<>(ID, (from, fromNode, side, to, toNode) ->
                    WarehouseConnectionRule.accepts(fromNode.kind(), side, toNode.kind())
                            && railSideAccepted(fromNode, side, toNode)
                            && fromNode.linksTowards(side, toNode), WarehouseNetwork::new);

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

    /** An indexed member, read through {@link StorageNodeBlockEntity#stock()}. */
    private record Source(BlockPos pos) {
    }

    /** The slots of a source, or null when its chunk is not loaded or it holds no stock. */
    private ItemView handlerOf(Source source) {
        return source != null && loaded(source.pos()) instanceof StorageNodeBlockEntity node ? node.stock() : null;
    }

    private final Map<BlockPos, Source> sources = new LinkedHashMap<>();
    /** Members that hold no stock, such as rails. Never sources. */
    private final Set<BlockPos> connectors = new HashSet<>();
    /** Elevator segments, a subset of the connectors; grouped into shafts on the tick after a change. */
    private final Set<NodePos> elevators = new HashSet<>();
    private Map<NodePos, Shaft> shaftFloors = Map.of();
    private boolean shaftsDirty;
    /** The zone each zoned stock node carries. Unzoned nodes are absent. */
    private final Map<BlockPos, ZoneDefinition> zoneOf = new LinkedHashMap<>();
    /** Set when a zone or rule changed; stations with waiting stacks retry on the next tick. */
    private boolean zonesDirty;
    /** Inbound stations: members that are neither sources nor travel nodes. */
    private final Set<BlockPos> stations = new HashSet<>();
    /** Shuttle garages: members that are neither sources nor travel nodes. */
    private final Set<BlockPos> garages = new HashSet<>();
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
    /** The state of each rule after its last order attempt, when not ok. */
    private final Map<ItemKey, String> ruleStates = new HashMap<>();
    private int ruleTimer;
    private int outboundTimer;
    private int fieldTimer;
    private final ShuttleFleet fleet = new ShuttleFleet(this);
    /** Set by every membership or source change; the controller's span is recomputed once on the next tick. */
    private boolean spanDirty = true;
    private int snapshotTimer;
    /**
     * Set when a Packing Station joins or leaves, or the controller's Wireless
     * Upgrade or power changes; the site record learns it next tick.
     */
    private boolean wirelessDirty = true;
    private long snapshotVersion = -1;

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
            if (node.stock() != null) {
                addSource(blockPos);
            }
            rememberZone(blockPos, node);
            // A new storage block may give waiting stacks a place to go.
            zonesDirty = true;
        } else if (node.kind() == WarehouseConnectionRule.NodeKind.STATION) {
            stations.add(blockPos);
            pendingStations.add(blockPos);
        } else if (node instanceof ShuttleGarageBlockEntity garage) {
            garages.add(blockPos);
            fleet.adoptGarage(blockPos, garage.slots(), garage.isActive());
        } else {
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
        spanDirty = true;
        wirelessDirty |= node instanceof PackingStationBlockEntity || node instanceof WarehouseControllerBlockEntity;
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
        held.remove(blockPos);
        if (node instanceof WarehouseControllerBlockEntity) {
            controllers--;
            if (blockPos.equals(controllerPos)) {
                controllerPos = null;
            }
        }
        lampsDirty = true;
        spanDirty = true;
        wirelessDirty |= node instanceof PackingStationBlockEntity || node instanceof WarehouseControllerBlockEntity;
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
        that.held.forEach((pos, loads) -> held.computeIfAbsent(pos, p -> new ArrayList<>()).addAll(loads));
        zoneOf.putAll(that.zoneOf);
        that.sources.forEach((pos, source) -> {
            sources.put(pos, source);
            index.update(pos, that.index.snapshot(pos));
        });
        pollOrderStale = true;
        lampsDirty = true;
        spanDirty = true;
        wirelessDirty = true;
        fleet.absorb(that.fleet);
    }

    /** A garage's shuttles or pause state changed. */
    public void garageChanged(ShuttleGarageBlockEntity garage) {
        fleet.adoptGarage(garage.getBlockPos(), garage.slots(), garage.isActive());
    }

    /** Brings every shuttle of the garage inside; cargo drops at the garage and unloaded jobs are queued again. */
    public void recallGarage(BlockPos pos) {
        fleet.recall(pos);
    }

    /** Shuttles of one garage that are out on a job. */
    public int garageOut(BlockPos pos) {
        return fleet.outOf(pos);
    }

    /** Slots of one garage whose shuttle is out, one bit per slot. */
    public int garageOutSlots(BlockPos pos) {
        return fleet.outSlots(pos);
    }

    /** Jobs waiting for a shuttle. */
    public int queuedJobs() {
        return fleet.queued();
    }

    /** Jobs shuttles carry right now, every load of a Cargo Hold counted. */
    public int ridingJobs() {
        return fleet.riding();
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
        return loaded(pos) instanceof HeavyRackBlockEntity;
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
        return key.stack(1).typeHolder().tags().map(tag -> tag.location().toString()).collect(Collectors.toSet());
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

    /** The target lists in order: exact matches, tag matches, then general storage; holders first within each. */
    private List<List<BlockPos>> targetTiers(ItemKey key, PutAwayPlan.Tiers<BlockPos> tiers) {
        Set<BlockPos> general = new LinkedHashSet<>();
        for (Source source : sources.values()) {
            ZoneDefinition zone = zoneOf.get(source.pos());
            if (zone == null || zone.filter().isEmpty()) {
                general.add(source.pos());
            }
        }
        List<BlockPos> holders = new ArrayList<>();
        for (BlockPos pos : index.sourcesHolding(key)) {
            if (sources.containsKey(pos)) {
                holders.add(pos);
            }
        }
        return PutAwayPlan.targets(tiers, general, holders);
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
        if (level == null || !level.isLoaded(pos)) {
            return result;
        }
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
        ItemView handler = handlerOf(source);
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
    public double liftSpeed() {
        return StorageStatsManager.INSTANCE.liftSpeed();
    }

    @Override
    public Shaft shaftOf(NodePos node) {
        return shaftFloors.get(node);
    }

    // Requests and shipments

    /**
     * Loads that did not fit into a destination's delivery buffer yet, per
     * destination: what a shuttle carried when its terminal lost its rail.
     */
    private final Map<BlockPos, List<HeldLoad>> held = new HashMap<>();

    private record HeldLoad(PickJob job, ItemStack stack) {
    }

    /** Whether some garage shuttle can drive to a rail beside the position. */
    public boolean canReach(BlockPos pos) {
        return fleet.canReach(pos);
    }

    /** Delivers items into an outbound station's buffer, planned and refused like a request. */
    public String feedStation(OutboundStationBlockEntity station, ItemKey key, int amount) {
        return planAndStart(station.getBlockPos(), key, amount, 0).refusal();
    }

    /** The outcome of a shipping request: a refusal key, or the shipment on its Packing Station. */
    public record Shipping(String refusal, Optional<PackingStationBlockEntity.Shipment> shipment, BlockPos packingStation) {
        public boolean shipped() {
            return refusal.isEmpty();
        }
    }

    /**
     * Ships items to a data member through the Packing Station with the shortest
     * shuttle trip from the racks holding them.
     *
     * @return refusal keys: {@code offline}, {@code no_packing_station}, {@code out_of_stock},
     *         {@code needs_shuttle} or {@code no_rail}
     */
    public Shipping ship(BlockPos target, ItemKey key, int amount) {
        return ship(target, key, amount, 0);
    }

    /** Like {@link #ship(BlockPos, ItemKey, int)}, with pick jobs tagged by {@code requestId} so they can be dropped. */
    public Shipping ship(BlockPos target, ItemKey key, int amount, int requestId) {
        if (status() != Status.ONLINE || level == null) {
            return new Shipping("offline", Optional.empty(), null);
        }
        List<PackingStationBlockEntity> candidates = new ArrayList<>();
        for (BlockPos pos : stations) {
            if (loaded(pos) instanceof PackingStationBlockEntity point
                    && DataNetworks.networkAt(level, pos).filter(network -> network.reaches(target)).isPresent()) {
                candidates.add(point);
            }
        }
        return shipThrough(candidates, target, key, amount, requestId, "no_packing_station");
    }

    /**
     * Sends items over the wireless line to a Packing Station of another site:
     * picked into this warehouse's Packing Station with the shortest shuttle
     * trip, with or without a data line, then moved by that station. The
     * refusals name this warehouse as "there", since the site that asks for the
     * items shows them.
     *
     * @return refusal keys: {@code offline}, {@code no_wireless_there} (no Wireless Upgrade), {@code no_power_there},
     *         {@code no_packing_there}, {@code out_of_stock}, {@code needs_shuttle} or {@code no_rail}
     */
    public Shipping shipWireless(BlockPos receiver, ItemKey key, int amount, int requestId) {
        if (status() != Status.ONLINE || level == null) {
            return new Shipping("offline", Optional.empty(), null);
        }
        Optional<WarehouseControllerBlockEntity> controller = controller();
        if (controller.isEmpty()) {
            return new Shipping("offline", Optional.empty(), null);
        }
        if (!controller.get().capable()) {
            return new Shipping(controller.get().hasWirelessUpgrade() ? "no_power_there" : "no_wireless_there", Optional.empty(), null);
        }
        List<PackingStationBlockEntity> candidates = new ArrayList<>();
        for (BlockPos pos : packingStations()) {
            if (!pos.equals(receiver) && loaded(pos) instanceof PackingStationBlockEntity point) {
                candidates.add(point);
            }
        }
        return shipThrough(candidates, receiver, key, amount, requestId, "no_packing_there");
    }

    /** Plans the picks into the candidate with the shortest shuttle trip and opens its shipment to the target. */
    private Shipping shipThrough(List<PackingStationBlockEntity> candidates, BlockPos target, ItemKey key, int amount, int requestId,
                                 String none) {
        candidates.sort(java.util.Comparator.comparing(point -> point.getBlockPos().asLong()));
        List<BlockPos> racks = sourcesHolding(key).stream().filter(this::isRack).toList();
        Optional<PackingStationBlockEntity> chosen = PackingStationChoice.choose(candidates, point -> trip(racks, point.getBlockPos()));
        if (chosen.isEmpty()) {
            return new Shipping(none, Optional.empty(), null);
        }
        BlockPos destination = chosen.get().getBlockPos();
        boolean reachable = fleet.canReach(destination);
        RequestPlan.Plan<BlockPos> plan = RequestPlan.plan(amount, key.stack(1).getMaxStackSize(), reachable, stockOf(key));
        if (plan.jobs().isEmpty()) {
            String refusal = plan.needsShuttle() ? (fleet.hasShuttles() && !reachable ? "no_rail" : "needs_shuttle") : "out_of_stock";
            return new Shipping(refusal, Optional.empty(), destination);
        }
        PackingStationBlockEntity.Shipment shipment = chosen.get().addShipment(target, key, plan.planned());
        startJobs(plan, requestId, key, destination);
        return new Shipping("", Optional.of(shipment), destination);
    }

    /** How many of an item the sources hold right now. */
    @Override
    public long available(ItemKey key) {
        long sum = 0;
        for (RequestPlan.Stock<BlockPos> stock : stockOf(key)) {
            sum += stock.count();
        }
        return sum;
    }

    /**
     * How many of an item the sources hold, the numbers {@link #available} adds
     * up, without asking the fleet whether each source can be reached. Cheap
     * enough for a GUI that refreshes its counts every second.
     */
    public long stockCount(ItemKey key) {
        long sum = 0;
        for (BlockPos pos : index.sourcesHolding(key)) {
            Integer count = index.snapshot(pos).get(key);
            if (count != null && count > 0 && sources.containsKey(pos)) {
                sum += count;
            }
        }
        return sum;
    }

    /** Open incoming wireless transfers per Packing Station of this warehouse, for the choice of the receiver. */
    private final Map<BlockPos, Integer> incoming = new HashMap<>();

    /** How many open wireless transfers are on their way to the Packing Station at that position. */
    public int incomingWireless(BlockPos station) {
        return incoming.getOrDefault(station, 0);
    }

    /** The Packing Stations of this warehouse. */
    public List<BlockPos> packingStations() {
        List<BlockPos> points = new ArrayList<>();
        if (level == null) {
            return points;
        }
        for (BlockPos pos : stations) {
            if (loaded(pos) instanceof PackingStationBlockEntity) {
                points.add(pos);
            }
        }
        points.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        return points;
    }

    /** The controller of an online warehouse, when its chunk is loaded. */
    public Optional<WarehouseControllerBlockEntity> controller() {
        if (status() != Status.ONLINE || controllerPos == null) {
            return Optional.empty();
        }
        return loaded(controllerPos) instanceof WarehouseControllerBlockEntity controller ? Optional.of(controller) : Optional.empty();
    }

    /** The position of the single controller, empty unless online. */
    public Optional<BlockPos> controllerPos() {
        return status() == Status.ONLINE ? Optional.ofNullable(controllerPos) : Optional.empty();
    }

    /** Drops the queued pick jobs of a request; loads already taken keep going. */
    public void dropQueuedFor(int requestId) {
        fleet.removeQueued(requestId);
    }

    /** Rail steps from the nearest rack to the Packing Station, 0 without racks, unreachable without a path. */
    private int trip(List<BlockPos> racks, BlockPos packingStation) {
        if (racks.isEmpty()) {
            return 0;
        }
        Set<NodePos> goal = dockRails(Conversions.toNodePos(packingStation));
        int best = PackingStationChoice.UNREACHABLE;
        for (BlockPos rack : racks) {
            for (NodePos start : dockRails(Conversions.toNodePos(rack))) {
                List<NodePos> path = RailPaths.find(start, goal::contains, this::travelNeighbours);
                if (!path.isEmpty()) {
                    best = Math.min(best, path.size() - 1);
                }
            }
        }
        return best;
    }

    // Outbound stations

    /** Tops every outbound station's buffer up to its keep amounts and records what could not be filled. */
    private void tickOutbound() {
        if (status() != Status.ONLINE || level == null) {
            return;
        }
        for (BlockPos pos : new ArrayList<>(stations)) {
            if (!level.isLoaded(pos) || !(loaded(pos) instanceof OutboundStationBlockEntity station)) {
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

    // Stock rules

    /** The controller's stock rules, present while a controller is in the network and loaded. */
    public Optional<StockRules> stockRules() {
        if (controllerPos == null || level == null || !level.isLoaded(controllerPos)) {
            return Optional.empty();
        }
        return loaded(controllerPos) instanceof WarehouseControllerBlockEntity controller
                ? Optional.of(controller.rules()) : Optional.empty();
    }

    public void setStockRule(ItemKey key, int min, int max) {
        stockRules().ifPresent(rules -> {
            rules.set(new StockRule(key, min, max));
            ruleStates.remove(key);
        });
        ruleTimer = StorageStatsManager.INSTANCE.stockRuleInterval();
    }

    public boolean removeStockRule(ItemKey key) {
        ruleStates.remove(key);
        return stockRules().map(rules -> rules.remove(key)).orElse(false);
    }

    /** Orders for every rule whose stock plus open orders on the line's CPUs fell below its minimum. */
    private void tickStockRules() {
        if (status() != Status.ONLINE || level == null) {
            return;
        }
        Optional<StockRules> rules = stockRules();
        if (rules.isEmpty()) {
            return;
        }
        Optional<DataNetwork> line = CpuOrders.network(level, this);
        for (StockRule rule : rules.get().all()) {
            int open = line.map(network -> CpuOrders.openAmountFor(level, network, rule.key())).orElse(0);
            long have = index.count(rule.key()) + open;
            if (have >= rule.min()) {
                ruleStates.remove(rule.key());
                continue;
            }
            if (line.isEmpty() || CpuOrders.cpus(level, line.get()).isEmpty()) {
                ruleStates.put(rule.key(), "no_cpu");
                continue;
            }
            int wanted = (int) Math.min(Integer.MAX_VALUE, rule.max() - have);
            CpuOrders.Placed placed = CpuOrders.create(level, line.get(), rule.key(), wanted, OrderBook.RULE_ORIGIN);
            if (placed.made()) {
                ruleStates.remove(rule.key());
            } else if (placed.refusal().equals("no_producer")) {
                ruleStates.put(rule.key(), "no_producer");
            }
        }
    }

    /** The state of a rule: ordering, no CPU, no producer, or ok. */
    public String ruleState(StockRule rule) {
        if (level != null) {
            Optional<DataNetwork> line = CpuOrders.network(level, this);
            if (line.isPresent() && CpuOrders.openAmountFor(level, line.get(), rule.key()) > 0) {
                return "ordering";
            }
        }
        return ruleStates.getOrDefault(rule.key(), "ok");
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
        if (level == null) {
            return "offline";
        }
        Optional<DataNetwork> line = CpuOrders.network(level, this);
        return line.map(network -> CpuOrders.create(level, network, key, amount, 0).refusal()).orElse("no_cpu");
    }

    private List<RequestPlan.Stock<BlockPos>> stockOf(ItemKey key) {
        List<RequestPlan.Stock<BlockPos>> stock = new ArrayList<>();
        for (BlockPos pos : index.sourcesHolding(key)) {
            Integer count = index.snapshot(pos).get(key);
            if (count == null || count <= 0 || !sources.containsKey(pos)) {
                continue;
            }
            stock.add(new RequestPlan.Stock<>(pos, count, fleet.canReach(pos)));
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

    /** Queues one shuttle pick per planned job. */
    private void startJobs(RequestPlan.Plan<BlockPos> plan, int requestId, ItemKey key, BlockPos destination) {
        for (RequestPlan.Job<BlockPos> job : plan.jobs()) {
            fleet.requestPick(new PickJob(requestId, key, job.count(), job.source(), destination));
        }
    }

    @Override
    public void pickLoaded(PickJob job, int loaded) {
        if (loaded < job.amount() && loaded(job.terminal()) instanceof PackingStationBlockEntity point) {
            point.shortfall(job.key(), job.amount() - loaded);
        }
    }

    @Override
    public void pickDropped(PickJob job, int count) {
        if (loaded(job.terminal()) instanceof PackingStationBlockEntity point) {
            point.shortfall(job.key(), count);
        }
    }

    @Override
    public ItemStack deliverTo(PickJob job, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        BlockEntity destination = loaded(job.terminal());
        if (!(destination instanceof DeliveryTarget target)) {
            Block.popResource(level, job.terminal(), stack);
            return ItemStack.EMPTY;
        }
        return target.deliveries().insertItem(stack, false);
    }

    @Override
    public void deliverInstant(PickJob job, ItemStack stack) {
        ItemStack left = deliverTo(job, stack);
        if (!left.isEmpty()) {
            held.computeIfAbsent(job.terminal(), pos -> new ArrayList<>()).add(new HeldLoad(job, left));
        }
    }

    /** Retries the held loads of a destination after its delivery buffer changed. */
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
        held.clear();
        pendingStations.clear();
        blockedStations.clear();
        zoneOf.clear();
        dirtyQueue.clear();
        dirtySet.clear();
        pollOrder.clear();
        controllers = 0;
    }

    private void addSource(BlockPos pos) {
        if (!sources.containsKey(pos)) {
            sources.put(pos, new Source(pos));
            pollOrderStale = true;
        }
        markDirty(pos);
    }

    private void dropSource(BlockPos pos) {
        if (sources.remove(pos) != null) {
            index.remove(pos);
            dirtySet.remove(pos);
            pollOrderStale = true;
        }
    }

    /**
     * The block entity at a position whose chunk is loaded, else null. Never
     * loads a chunk: a load from a membership hook while chunks unload brings
     * the chunk back and the server never finishes stopping.
     */
    private BlockEntity loaded(BlockPos pos) {
        return level != null && level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
    }

    // Events and polling

    @Override
    protected void handle(WarehouseEvent event) {
        if (event instanceof WarehouseEvent.Dirty dirty) {
            if (sources.containsKey(dirty.pos())) {
                markDirty(dirty.pos());
            }
        } else if (event instanceof WarehouseEvent.OutputChanged output) {
            retryHeld(output.pos());
        } else if (event instanceof WarehouseEvent.Intake intake) {
            if (stations.contains(intake.pos())) {
                pendingStations.add(intake.pos());
            }
        } else if (event instanceof WarehouseEvent.LiftUpgradesChanged lift) {
            // The car reads its shaft's upgrades when the shafts are refreshed next tick.
            if (elevators.contains(Conversions.toNodePos(lift.pos()))) {
                shaftsDirty = true;
            }
        } else if (event instanceof WarehouseEvent.ZoneChanged zoned) {
            if (loaded(zoned.pos()) instanceof StorageNodeBlockEntity node && node.holdsStock()) {
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
        if (spanDirty && level != null && controllers == 1 && controllerPos != null) {
            updateSpan();
        }
        if (wirelessDirty && level != null && controllers == 1) {
            controller().ifPresent(controller -> {
                wirelessDirty = false;
                io.github.dirnei.mc.servus.storage.site.SiteRegistry.of(level).updateWireless(controller, controller.hasWirelessUpgrade(),
                        controller.poweredForWireless(), !packingStations().isEmpty());
            });
        }
        if (level != null && controllers == 1 && ++snapshotTimer >= StorageStatsManager.INSTANCE.siteSnapshotInterval()) {
            snapshotTimer = 0;
            snapshotStock();
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
            if (++outboundTimer >= StorageStatsManager.INSTANCE.outboundCheckInterval()) {
                outboundTimer = 0;
                tickOutbound();
            }
            if (++ruleTimer >= StorageStatsManager.INSTANCE.stockRuleInterval()) {
                ruleTimer = 0;
                tickStockRules();
            }
            if (++fieldTimer >= StorageStatsManager.INSTANCE.fieldCheckInterval()) {
                fieldTimer = 0;
                tickFields();
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
            if (loaded(pos) instanceof InboundStationBlockEntity station) {
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
            station.claim(slot);
            fleet.requestPutAway(new PutAwayJob(pos, slot, key, stack.getCount(), target.get()));
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
                if (handlerOf(source) != null) {
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
        ItemView handler = handlerOf(source);
        return handler != null && handler.insertItem(stack, true).getCount() < stack.getCount();
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

    @Override
    public Optional<BlockPos> putAwayTarget(ItemStack stack, Predicate<BlockPos> allowed) {
        ItemKey key = ItemKey.of(stack);
        for (List<BlockPos> tier : targetTiers(key, tiers(key))) {
            for (BlockPos pos : tier) {
                if (allowed.test(pos) && hasRoom(pos, stack)) {
                    return Optional.of(pos);
                }
            }
        }
        return Optional.empty();
    }

    // Fields

    /**
     * Looks at the Field Heads of the fields on this warehouse's rails and
     * queues a field run for each that has work, when a harvester can take it.
     * Polls the few loaded heads; touches no blocks.
     */
    private void tickFields() {
        if (status() != Status.ONLINE) {
            return;
        }
        boolean harvesters = fleet.hasFieldShuttles(JobNeeds.HARVESTER);
        boolean spreaders = fleet.hasFieldShuttles(JobNeeds.SPREADER);
        if (!harvesters && !spreaders) {
            return;
        }
        for (FieldHeadBlockEntity head : FieldHeads.heads(level)) {
            if (head.status() != FieldStatus.SERVING || head.rails().isEmpty()
                    || !members().contains(Conversions.toNodePos(head.rails().iterator().next()))) {
                continue;
            }
            BlockPos pos = head.getBlockPos();
            if (harvesters && !fleet.hasFieldJob(pos) && fleet.fieldReachable(pos, JobNeeds.HARVESTER)) {
                boolean seeds = head.plantMode() && available(ItemKey.of(head.crop())) > 0;
                if (FieldDue.due(head.harvestDue(), head.plantMode(), head.hydrated(), head.tillableCount(), head.plantableCount(), seeds)) {
                    fleet.requestField(new FieldJob(pos));
                }
            }
            if (spreaders && head.fertilizing() && !fleet.hasFertilizeJob(pos) && fleet.fieldReachable(pos, JobNeeds.SPREADER)) {
                long now = level.getGameTime();
                if (head.fertilizeDue(now, available(ItemKey.of(head.fertilizer())))) {
                    fleet.requestFertilize(new FertilizeJob(pos));
                    head.fertilizeCalled(now);
                }
            }
        }
    }

    /** Whether some garage of this warehouse holds a shuttle for field jobs with these needs. */
    public boolean hasFieldShuttles(JobNeeds needs) {
        return fleet.hasFieldShuttles(needs);
    }

    /** Whether a shuttle with these needs can drive to that head's field, without a lift when it is too wide for one. */
    public boolean fieldReachable(BlockPos head, JobNeeds needs) {
        return fleet.fieldReachable(head, needs);
    }

    /** Whether a fertilize run for that head is queued or under way. */
    public boolean hasFertilizeJob(BlockPos head) {
        return fleet.hasFertilizeJob(head);
    }

    /** The item of the Spreader dosing that head's field right now, or empty. */
    public Optional<ItemStack> spreadWorker(BlockPos head) {
        return fleet.spreadWorker(head);
    }

    /** Whether some garage of this warehouse holds a shuttle with a Harvester. */
    public boolean hasFieldShuttles() {
        return fleet.hasFieldShuttles();
    }

    /** Whether a Harvester shuttle can drive to that head's field, without a lift when it is too wide for one. */
    public boolean fieldReachable(BlockPos head) {
        return fleet.fieldReachable(head);
    }

    /** Whether a field run for that head is queued or under way. */
    public boolean hasFieldJob(BlockPos head) {
        return fleet.hasFieldJob(head);
    }

    /** The item of the shuttle working that head's field right now, or empty. */
    public Optional<ItemStack> fieldWorker(BlockPos head) {
        return fleet.fieldWorker(head);
    }

    /** Inserts into exactly that source; returns what did not fit. */
    @Override
    public ItemStack insertAt(BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        Source source = sources.get(pos);
        ItemView handler = handlerOf(source);
        if (handler == null) {
            return stack;
        }
        ItemStack left = handler.insertItem(stack, false);
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
        if (loaded(pos) instanceof InboundStationBlockEntity station) {
            left = station.buffer().insertItem(stack, false);
        }
        if (!left.isEmpty()) {
            Block.popResource(level, pos, left);
        }
    }

    @Override
    public void releaseClaim(BlockPos station, int slot) {
        if (loaded(station) instanceof InboundStationBlockEntity be) {
            be.release(slot);
            pendingStations.add(station);
        }
    }

    /** Takes up to amount of the key out of a station's buffer and frees the slot's claim. */
    @Override
    public ItemStack takeFromStation(BlockPos pos, int slot, ItemKey key, int amount) {
        if (!(loaded(pos) instanceof InboundStationBlockEntity station)) {
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

    /**
     * Hands the controller the chunks its site spans: every member of this
     * network, merged with the chunks it knew that are not loaded. In memory
     * only, no block is looked at.
     */
    private void updateSpan() {
        if (!(loaded(controllerPos) instanceof WarehouseControllerBlockEntity controller)) {
            return;
        }
        spanDirty = false;
        Set<Long> current = new LinkedHashSet<>();
        for (NodePos member : members()) {
            current.add(SiteChunks.ofBlock(member.x(), member.z()));
        }
        controller.setSpan(SiteChunks.merge(controller.span(), current,
                key -> level.getChunkSource().hasChunk(SiteChunks.x(key), SiteChunks.z(key))));
    }

    /**
     * Reports the index as the site's last known stock, only while every chunk
     * of the site is loaded, so a site that unloads piece by piece keeps the
     * stock it had as a whole.
     */
    private void snapshotStock() {
        if (index.version() == snapshotVersion) {
            return;
        }
        controller().ifPresent(controller -> {
            for (long key : controller.span()) {
                if (!level.getChunkSource().hasChunk(SiteChunks.x(key), SiteChunks.z(key))) {
                    return;
                }
            }
            snapshotVersion = index.version();
            io.github.dirnei.mc.servus.storage.site.SiteRegistry.of(level).updateStock(controller, index.entries(), level.getGameTime());
        });
    }

    /** The controller's Wireless Upgrade or powered state changed: the site record learns it next tick. */
    public void wirelessChanged() {
        wirelessDirty = true;
    }

    /**
     * Whether the site's own work runs: shuttles out or jobs waiting, inbound
     * buffers to drain, deliveries held. A woken site stays loaded
     * while this is true.
     */
    public boolean siteBusy() {
        return fleet.busy() > 0 || fleet.queued() > 0 || !pendingStations.isEmpty()
                || held.values().stream().anyMatch(loads -> !loads.isEmpty());
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
            boolean lit = online;
            if (state.hasProperty(StorageNodeBlocks.ONLINE) && state.getValue(StorageNodeBlocks.ONLINE) != lit) {
                level.setBlock(pos, state.setValue(StorageNodeBlocks.ONLINE, lit), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private void poll(BlockPos pos) {
        Source source = sources.get(pos);
        ItemView handler = handlerOf(source);
        long before = total(index.snapshot(pos));
        if (handler == null) {
            index.remove(pos);
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

    /**
     * Takes up to {@code amount} (at most one stack) of the key straight out of
     * the racks, for the op command {@code /servus_warehouse take} only; players
     * and machines get items by shuttle.
     */
    public ItemStack takeDirect(ItemKey key, int amount) {
        ItemStack result = ItemStack.EMPTY;
        int wanted = Math.min(amount, key.stack(1).getMaxStackSize());
        for (BlockPos pos : List.copyOf(index.sourcesHolding(key))) {
            if (wanted <= 0) {
                break;
            }
            ItemStack taken = extractFrom(pos, key, wanted);
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
}
