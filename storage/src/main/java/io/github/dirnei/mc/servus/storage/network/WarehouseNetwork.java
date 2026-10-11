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
import java.util.function.Predicate;
import io.github.dirnei.mc.servus.storage.index.CandidateNeighbours;
import io.github.dirnei.mc.servus.storage.index.WarehouseIndex;
import io.github.dirnei.mc.servus.storage.shuttle.PickJob;
import io.github.dirnei.mc.servus.storage.shuttle.PutAwayJob;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleFleet;
import io.github.dirnei.mc.servus.storage.shuttle.Shafts;
import io.github.dirnei.mc.servus.storage.shuttle.Shafts.Shaft;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.PackingStationChoice;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.storage.shuttle.RailPaths;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
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
    private final ShuttleFleet fleet = new ShuttleFleet(this);

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
            fleet.adoptGarage(blockPos, garage.profiles(), garage.isActive());
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
        held.remove(blockPos);
        deferred.remove(blockPos);
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
        that.held.forEach((pos, loads) -> held.computeIfAbsent(pos, p -> new ArrayList<>()).addAll(loads));
        that.deferred.forEach((pos, picks) -> deferred.computeIfAbsent(pos, p -> new ArrayList<>()).addAll(picks));
        that.deferred.clear();
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

    /** A garage's shuttles or pause state changed. */
    public void garageChanged(ShuttleGarageBlockEntity garage) {
        fleet.adoptGarage(garage.getBlockPos(), garage.profiles(), garage.isActive());
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
                if (loaded(pos) instanceof CrateBlockEntity crate && crate.isEmpty()) {
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

    // Requests and shipments

    /** Instant loads that did not fit into a destination's delivery buffer yet, per destination. */
    private final Map<BlockPos, List<HeldLoad>> held = new HashMap<>();

    private record HeldLoad(PickJob job, ItemStack stack) {
    }

    /**
     * Instant picks for a Packing Station that wait in their source until the
     * station's To network buffer has room, per station, oldest first.
     */
    private final Map<BlockPos, List<DeferredPick>> deferred = new HashMap<>();

    private static final class DeferredPick {
        private final PickJob job;
        private int left;

        private DeferredPick(PickJob job) {
            this.job = job;
            this.left = job.amount();
        }
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
        candidates.sort(java.util.Comparator.comparing(point -> point.getBlockPos().asLong()));
        List<BlockPos> racks = sourcesHolding(key).stream().filter(this::isRack).toList();
        Optional<PackingStationBlockEntity> chosen = PackingStationChoice.choose(candidates, point -> trip(racks, point.getBlockPos()));
        if (chosen.isEmpty()) {
            return new Shipping("no_packing_station", Optional.empty(), null);
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
    public long available(ItemKey key) {
        long sum = 0;
        for (RequestPlan.Stock<BlockPos> stock : stockOf(key)) {
            sum += stock.count();
        }
        return sum;
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
        startPicks(plan, requestId, key, destination);
        releaseDeferred(destination);
    }

    private void startPicks(RequestPlan.Plan<BlockPos> plan, int requestId, ItemKey key, BlockPos destination) {
        for (RequestPlan.Job<BlockPos> job : plan.jobs()) {
            PickJob pick = new PickJob(requestId, key, job.count(), job.source(), destination);
            if (job.byShuttle()) {
                fleet.requestPick(pick);
            } else if (loaded(destination) instanceof PackingStationBlockEntity) {
                deferred.computeIfAbsent(destination, pos -> new ArrayList<>()).add(new DeferredPick(pick));
            } else {
                ItemStack taken = extractFrom(job.source(), key, job.count());
                pickLoaded(pick, taken.getCount());
                deliverInstant(pick, taken);
            }
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
        return ItemHandlerHelper.insertItem(target.deliveries(), stack, false);
    }

    @Override
    public void deliverInstant(PickJob job, ItemStack stack) {
        ItemStack left = deliverTo(job, stack);
        if (!left.isEmpty()) {
            held.computeIfAbsent(job.terminal(), pos -> new ArrayList<>()).add(new HeldLoad(job, left));
        }
    }

    /**
     * Picks for a Packing Station from crates and foreign inventories as far as
     * its To network buffer has room; the rest stays in its source until the
     * buffer drains. A source that holds less than planned cuts the shipment.
     */
    private void releaseDeferred(BlockPos stationPos) {
        List<DeferredPick> picks = deferred.get(stationPos);
        if (picks == null || level == null) {
            return;
        }
        if (!(loaded(stationPos) instanceof PackingStationBlockEntity station)) {
            return;
        }
        while (!picks.isEmpty()) {
            DeferredPick pick = picks.get(0);
            int room = station.outgoingRoom(pick.job.key(), pick.left);
            if (room <= 0) {
                break;
            }
            ItemStack taken = extractFrom(pick.job.source(), pick.job.key(), room);
            int got = taken.getCount();
            if (got < room) {
                station.shortfall(pick.job.key(), room - got);
            }
            deliverInstant(pick.job, taken);
            pick.left -= room;
            if (pick.left <= 0) {
                picks.remove(0);
            }
        }
        if (picks.isEmpty()) {
            deferred.remove(stationPos);
        }
    }

    /** Ends what deferred picks still owe as shortfalls, when this network goes away without handing them on. */
    private void dropDeferred() {
        deferred.forEach((pos, picks) -> {
            if (level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PackingStationBlockEntity station) {
                picks.forEach(pick -> station.shortfall(pick.job.key(), pick.left));
            }
        });
        deferred.clear();
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
        dropDeferred();
        pendingStations.clear();
        blockedStations.clear();
        zoneOf.clear();
        dirtyQueue.clear();
        dirtySet.clear();
        pollOrder.clear();
        controllers = 0;
    }

    private void addSource(BlockPos pos, boolean node) {
        // Storage blocks join as members or not at all, never as foreign inventories (a rack without a front rail).
        if (!node && loaded(pos) instanceof StorageNodeBlockEntity) {
            return;
        }
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
        if (connectors.contains(nodePos) || !pullsAround(nodePos)) {
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

    /** Whether the member at that position pulls the inventories touching it in; racks do not. */
    private boolean pullsAround(BlockPos pos) {
        Optional<StorageNodeBlockEntity> member = owner().graph().node(Conversions.toNodePos(pos));
        if (member.isPresent()) {
            return member.get().pullsNeighbours();
        }
        return !(loaded(pos) instanceof StorageNodeBlockEntity node) || node.pullsNeighbours();
    }

    /**
     * The block entity at a position whose chunk is loaded, else null. Never
     * loads a chunk: a load from a membership hook while chunks unload brings
     * the chunk back and the server never finishes stopping.
     */
    private BlockEntity loaded(BlockPos pos) {
        return level != null && level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
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
        for (NodePos member : memberPositions) {
            if (!pullsAround(Conversions.toBlockPos(member))) {
                connectorPositions.add(member);
            }
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
            releaseDeferred(output.pos());
        } else if (event instanceof WarehouseEvent.Intake intake) {
            if (stations.contains(intake.pos())) {
                pendingStations.add(intake.pos());
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
        if (loaded(pos) instanceof InboundStationBlockEntity station) {
            left = ItemHandlerHelper.insertItem(station.buffer(), stack, false);
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
        if (!source.node() && loaded(pos) instanceof StorageNodeBlockEntity) {
            // Added while its chunk was not loaded: storage blocks join as members or not at all.
            dropSource(pos);
            return;
        }
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
