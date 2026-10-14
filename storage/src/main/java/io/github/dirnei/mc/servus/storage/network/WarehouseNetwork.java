package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.field.FieldDue;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldHeads;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.SupplyBuffer;
import io.github.dirnei.mc.servus.storage.field.SupplyPlan;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.index.WarehouseIndex;
import io.github.dirnei.mc.servus.storage.order.StockRules;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.PackingStationChoice;
import io.github.dirnei.mc.servus.storage.packing.WirelessIncoming;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.shuttle.FertilizeJob;
import io.github.dirnei.mc.servus.storage.shuttle.FieldJob;
import io.github.dirnei.mc.servus.storage.shuttle.SupplyJob;
import io.github.dirnei.mc.servus.storage.shuttle.LiftCar;
import io.github.dirnei.mc.servus.storage.shuttle.PickJob;
import io.github.dirnei.mc.servus.storage.shuttle.PutAwayJob;
import io.github.dirnei.mc.servus.storage.shuttle.RailPaths;
import io.github.dirnei.mc.servus.storage.shuttle.RequestPlan;
import io.github.dirnei.mc.servus.storage.shuttle.Shafts;
import io.github.dirnei.mc.servus.storage.shuttle.Shafts.Shaft;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleFleet;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.storage.shuttle.module.JobNeeds;
import io.github.dirnei.mc.servus.storage.site.SiteChunks;
import io.github.dirnei.mc.servus.storage.site.SiteRegistry;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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

    private static final int BLOCKED_RETRY_TICKS = 20;

    /** A load that did not fit into its destination's delivery buffer yet. */
    private record HeldLoad(PickJob job, ItemStack stack) {
    }

    /** Indexed members, read through {@link StorageNodeBlockEntity#stock()}. */
    private final Set<BlockPos> sources = new LinkedHashSet<>();
    /** Members that hold no stock, such as rails. Never sources. */
    private final Set<BlockPos> connectors = new HashSet<>();
    /** Elevator segments, a subset of the connectors; grouped into shafts on the tick after a change. */
    private final Set<NodePos> elevators = new HashSet<>();
    private Map<NodePos, Shaft> shaftFloors = Map.of();
    private boolean shaftsDirty;
    private final WarehouseZones zones = new WarehouseZones();
    /** Set when a zone or room changed; stations with waiting stacks retry on the next tick. */
    private boolean zonesDirty;
    /** Inbound, outbound and Packing Stations. */
    private final Set<BlockPos> stations = new HashSet<>();
    private final Set<BlockPos> garages = new HashSet<>();
    /** Stations whose buffer changed; drained on the next tick. */
    private final Set<BlockPos> pendingStations = new LinkedHashSet<>();
    /** Stations holding a stack that found no room; retried on zone and room changes and on a slow timer. */
    private final Set<BlockPos> blockedStations = new LinkedHashSet<>();
    private int blockedRetry;
    /** Held loads per destination. */
    private final Map<BlockPos, List<HeldLoad>> held = new HashMap<>();
    private final WarehouseIndex<BlockPos, ItemKey> index = new WarehouseIndex<>();
    private final ArrayDeque<BlockPos> dirtyQueue = new ArrayDeque<>();
    private final Set<BlockPos> dirtySet = new HashSet<>();
    private final List<BlockPos> pollOrder = new ArrayList<>();
    private boolean pollOrderStale = true;
    private int cursor;
    private boolean lampsDirty;
    private ServerLevel level;
    private int controllers;
    /** The controller's position while exactly one is in the network. */
    private BlockPos controllerPos;
    private final StockRuleKeeper rules = new StockRuleKeeper(this);
    private int outboundTimer;
    private int fieldTimer;
    private final ShuttleFleet fleet = new ShuttleFleet(this);
    private boolean spanDirty = true;
    private int snapshotTimer;
    private long snapshotVersion = -1;
    /** Set when a Packing Station joins or leaves, or the controller's Wireless Upgrade or power changes. */
    private boolean wirelessDirty = true;

    public WarehouseNetwork(int id, NetworkSet<StorageNodeBlockEntity, WarehouseEvent, ?> owner) {
        super(id, owner);
    }

    /** A stock node may limit which of its sides take a rail; the rail side is asked from the stock node's view. */
    private static boolean railSideAccepted(StorageNodeBlockEntity from, Side side, StorageNodeBlockEntity to) {
        boolean fromRail = from.kind() == WarehouseConnectionRule.NodeKind.RAIL;
        boolean toRail = to.kind() == WarehouseConnectionRule.NodeKind.RAIL;
        if (fromRail == toRail) {
            return true;
        }
        if (toRail) {
            return !from.docksRail() || from.acceptsRail(side);
        }
        return !to.docksRail() || to.acceptsRail(side.opposite());
    }

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
            zones.set(blockPos, node.zoneDefinition());
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
        membershipChanged(node);
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
        zones.remove(blockPos);
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
        membershipChanged(node);
    }

    private void membershipChanged(StorageNodeBlockEntity node) {
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
        zones.absorb(that.zones);
        that.sources.forEach(pos -> {
            sources.add(pos);
            index.update(pos, that.index.snapshot(pos));
        });
        pollOrderStale = true;
        lampsDirty = true;
        spanDirty = true;
        wirelessDirty = true;
        fleet.absorb(that.fleet);
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
        zones.clear();
        dirtyQueue.clear();
        dirtySet.clear();
        pollOrder.clear();
        controllers = 0;
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
    public LiftCar lift(BlockPos bottom) {
        return fleet.lift(bottom);
    }

    /** The shuttles of one garage as clients draw them. */
    public List<ShuttleView> shuttleViews(BlockPos garage) {
        return fleet.views(garage);
    }

    /** Whether the source is a Heavy Duty Rack, which only shuttles serve. */
    @Override
    public boolean isRack(BlockPos pos) {
        return loaded(pos) instanceof HeavyRackBlockEntity;
    }

    /** The zoned blocks that take this key, by exact item and by tag. */
    private PutAwayPlan.Tiers<BlockPos> tiers(ItemKey key) {
        return zones.tiers(key, sources);
    }

    /** The target lists in order: exact matches, tag matches, then general storage; holders first within each. */
    private List<List<BlockPos>> targetTiers(ItemKey key, PutAwayPlan.Tiers<BlockPos> tiers) {
        List<BlockPos> holders = new ArrayList<>();
        for (BlockPos pos : index.sourcesHolding(key)) {
            if (sources.contains(pos)) {
                holders.add(pos);
            }
        }
        return zones.targets(tiers, sources, holders);
    }

    /** The first target in tier order that passes the test. */
    private Optional<BlockPos> firstTarget(ItemKey key, PutAwayPlan.Tiers<BlockPos> tiers, Predicate<BlockPos> test) {
        for (List<BlockPos> tier : targetTiers(key, tiers)) {
            for (BlockPos pos : tier) {
                if (test.test(pos)) {
                    return Optional.of(pos);
                }
            }
        }
        return Optional.empty();
    }

    /** Stock blocks per zone key, sorted by key. */
    public Map<String, Integer> zoneBlockCounts() {
        return zones.blockCounts();
    }

    /** One zone as its blocks carry it: the union of their filters. */
    public record ZoneSummary(String key, String display, int blocks, List<String> items, List<String> tags) {
    }

    /** Every zone of the warehouse, sorted by key. */
    public List<ZoneSummary> zoneSummaries() {
        return zones.summaries();
    }

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
        ItemView handler = handlerOf(pos);
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
            result = combine(result, taken);
            wanted -= taken.getCount();
        }
        if (!result.isEmpty()) {
            markDirty(pos);
        }
        return result;
    }

    /** Adds what was taken to the result so far; the first non empty stack becomes the result. */
    private static ItemStack combine(ItemStack result, ItemStack taken) {
        if (taken.isEmpty()) {
            return result;
        }
        if (result.isEmpty()) {
            return taken;
        }
        result.grow(taken.getCount());
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

    private static Shipping refused(String refusal) {
        return new Shipping(refusal, Optional.empty(), null);
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
            return refused("offline");
        }
        List<PackingStationBlockEntity> candidates = new ArrayList<>();
        for (BlockPos pos : stations) {
            if (loaded(pos) instanceof PackingStationBlockEntity point
                    && DataNetworks.networkAt(level, pos).filter(network -> network.reaches(target)).isPresent()) {
                candidates.add(point);
            }
        }
        return shipThrough(candidates, target, null, null, key, amount, requestId, "no_packing_station");
    }

    /**
     * Sends items over the wireless line to a Packing Station of another site:
     * picked into this warehouse's Packing Station with the shortest shuttle
     * trip, then moved by that station. The refusals call this warehouse
     * "there", as the asking site shows them.
     *
     * @return refusal keys: {@code offline}, {@code no_wireless_there} (no Wireless Upgrade), {@code no_power_there},
     *         {@code no_packing_there}, {@code out_of_stock}, {@code needs_shuttle} or {@code no_rail}
     */
    public Shipping shipWireless(BlockPos receiver, String receiverSite, ItemKey key, int amount, int requestId) {
        return shipWireless(receiver, receiverSite, key, amount, requestId, null);
    }

    /**
     * Like {@link #shipWireless(BlockPos, String, ItemKey, int, int)} for a
     * request at the Storage Terminal at {@code terminal}: the items land in the
     * receiver's To network buffer and go on to that terminal. Without a
     * terminal they are put away at the receiving site.
     */
    public Shipping shipWireless(BlockPos receiver, String receiverSite, ItemKey key, int amount, int requestId, BlockPos terminal) {
        if (status() != Status.ONLINE || level == null) {
            return refused("offline");
        }
        Optional<WarehouseControllerBlockEntity> controller = controller();
        if (controller.isEmpty()) {
            return refused("offline");
        }
        if (!controller.get().capable()) {
            return refused(controller.get().hasWirelessUpgrade() ? "no_power_there" : "no_wireless_there");
        }
        List<PackingStationBlockEntity> candidates = new ArrayList<>();
        for (BlockPos pos : packingStations()) {
            if (!pos.equals(receiver) && loaded(pos) instanceof PackingStationBlockEntity point) {
                candidates.add(point);
            }
        }
        return shipThrough(candidates, receiver, receiverSite, terminal, key, amount, requestId, "no_packing_there");
    }

    /**
     * Plans the picks into the candidate with the shortest shuttle trip and
     * opens its shipment to the target: a wireless one when {@code site}, the
     * receiving site's key, is given, else one over the data line.
     */
    private Shipping shipThrough(List<PackingStationBlockEntity> candidates, BlockPos target, String site, BlockPos terminal, ItemKey key,
                                 int amount, int requestId, String none) {
        candidates.sort(Comparator.comparing(point -> point.getBlockPos().asLong()));
        List<BlockPos> racks = sourcesHolding(key).stream().filter(this::isRack).toList();
        Optional<PackingStationBlockEntity> chosen = PackingStationChoice.choose(candidates, point -> trip(racks, point.getBlockPos()));
        if (chosen.isEmpty()) {
            return refused(none);
        }
        BlockPos destination = chosen.get().getBlockPos();
        boolean reachable = fleet.canReach(destination);
        RequestPlan.Plan<BlockPos> plan = plan(key, amount, reachable);
        if (plan.jobs().isEmpty()) {
            return new Shipping(refusal(plan, reachable), Optional.empty(), destination);
        }
        PackingStationBlockEntity.Shipment shipment = site == null ? chosen.get().addShipment(target, key, plan.planned())
                : chosen.get().addWirelessShipment(target, site, key, plan.planned(), terminal);
        startJobs(plan, requestId, key, destination);
        return new Shipping("", Optional.of(shipment), destination);
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

    /** How many of an item the sources hold right now. */
    @Override
    public long available(ItemKey key) {
        return stockCount(key);
    }

    /** How many of an item the sources hold, cheap enough for a GUI that refreshes every second. */
    public long stockCount(ItemKey key) {
        long sum = 0;
        for (Map.Entry<BlockPos, Integer> entry : stockAt(key).entrySet()) {
            sum += entry.getValue();
        }
        return sum;
    }

    /** The positive counts of the key per source, in index order. */
    private Map<BlockPos, Integer> stockAt(ItemKey key) {
        Map<BlockPos, Integer> stock = new LinkedHashMap<>();
        for (BlockPos pos : index.sourcesHolding(key)) {
            Integer count = index.snapshot(pos).get(key);
            if (count != null && count > 0 && sources.contains(pos)) {
                stock.put(pos, count);
            }
        }
        return stock;
    }

    private List<RequestPlan.Stock<BlockPos>> stockOf(ItemKey key) {
        List<RequestPlan.Stock<BlockPos>> stock = new ArrayList<>();
        stockAt(key).forEach((pos, count) -> stock.add(new RequestPlan.Stock<>(pos, count, fleet.canReach(pos))));
        return stock;
    }

    /**
     * How many open wireless transfers are on their way to the Packing Station
     * at that position, as the sending stations reported them.
     */
    public int incomingWireless(BlockPos station) {
        return level == null ? 0 : WirelessIncoming.count(level.dimension(), station);
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
        points.sort(Comparator.comparingLong(BlockPos::asLong));
        return points;
    }

    /** The controller of an online warehouse, when its chunk is loaded. */
    public Optional<WarehouseControllerBlockEntity> controller() {
        if (status() != Status.ONLINE || controllerPos == null) {
            return Optional.empty();
        }
        return loaded(controllerPos) instanceof WarehouseControllerBlockEntity controller ? Optional.of(controller) : Optional.empty();
    }

    /**
     * Draws FE from the controller's buffer for one of the warehouse's
     * consumers; refused while the warehouse is offline or its controller is
     * not loaded. A free draw is always paid.
     */
    public boolean drawPower(long amount) {
        if (amount <= 0) {
            return true;
        }
        return controller().map(controller -> controller.drawEnergy(amount)).orElse(false);
    }

    /** Whether the warehouse is in the no power state; an offline one or one without a loaded controller has none. */
    public boolean noPower() {
        return controller().map(WarehouseControllerBlockEntity::noPower).orElse(true);
    }

    /** The position of the single controller, empty unless online. */
    public Optional<BlockPos> controllerPos() {
        return status() == Status.ONLINE ? Optional.ofNullable(controllerPos) : Optional.empty();
    }

    /** Drops the queued pick jobs of a request; loads already taken keep going. */
    public void dropQueuedFor(int requestId) {
        fleet.removeQueued(requestId);
    }

    /** Tops every outbound station's buffer up to its keep amounts and records what could not be filled. */
    private void tickOutbound() {
        if (status() != Status.ONLINE) {
            return;
        }
        for (BlockPos pos : new ArrayList<>(stations)) {
            if (loaded(pos) instanceof OutboundStationBlockEntity station) {
                restock(pos, station);
            }
        }
    }

    private void restock(BlockPos pos, OutboundStationBlockEntity station) {
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

    /** The controller's stock rules, present while a controller is in the network and loaded. */
    public Optional<StockRules> stockRules() {
        return controllerPos != null && loaded(controllerPos) instanceof WarehouseControllerBlockEntity controller
                ? Optional.of(controller.rules()) : Optional.empty();
    }

    public void setStockRule(ItemKey key, int min, int max) {
        rules.set(key, min, max);
    }

    public boolean removeStockRule(ItemKey key) {
        return rules.remove(key);
    }

    public List<RuleRow> ruleRows() {
        return rules.rows();
    }

    /** Creates a work order by hand; returns the refusal key or an empty string. */
    public String order(ItemKey key, int amount) {
        if (status() != Status.ONLINE || level == null) {
            return "offline";
        }
        Optional<DataNetwork> line = CpuOrders.network(level, this);
        return line.map(network -> CpuOrders.create(level, network, key, amount, 0).refusal()).orElse("no_cpu");
    }

    /** What a plan and start did: how many are on their way, or why nothing happened. */
    private record Started(String refusal, int planned) {
    }

    private Started planAndStart(BlockPos destination, ItemKey key, int amount, int requestId) {
        if (status() != Status.ONLINE) {
            return new Started("offline", 0);
        }
        boolean reachable = fleet.canReach(destination);
        RequestPlan.Plan<BlockPos> plan = plan(key, amount, reachable);
        if (plan.jobs().isEmpty()) {
            return new Started(refusal(plan, reachable), 0);
        }
        startJobs(plan, requestId, key, destination);
        return new Started("", plan.planned());
    }

    private RequestPlan.Plan<BlockPos> plan(ItemKey key, int amount, boolean destinationReachable) {
        return RequestPlan.plan(amount, key.stack(1).getMaxStackSize(), destinationReachable, stockOf(key));
    }

    /** Why a plan without jobs failed: {@code no_rail}, {@code needs_shuttle} or {@code out_of_stock}. */
    private String refusal(RequestPlan.Plan<BlockPos> plan, boolean destinationReachable) {
        if (!plan.needsShuttle()) {
            return "out_of_stock";
        }
        return fleet.hasShuttles() && !destinationReachable ? "no_rail" : "needs_shuttle";
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
        if (!(loaded(job.terminal()) instanceof DeliveryTarget target)) {
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

    private void addSource(BlockPos pos) {
        if (sources.add(pos)) {
            pollOrderStale = true;
        }
        markDirty(pos);
    }

    private void dropSource(BlockPos pos) {
        if (sources.remove(pos)) {
            index.remove(pos);
            dirtySet.remove(pos);
            pollOrderStale = true;
        }
    }

    /** The slots of a source, or null when it is no source, its chunk is not loaded or it holds no stock. */
    private ItemView handlerOf(BlockPos pos) {
        return sources.contains(pos) && loaded(pos) instanceof StorageNodeBlockEntity node ? node.stock() : null;
    }

    /**
     * The block entity at a position whose chunk is loaded, else null. Never
     * loads a chunk: a load from a membership hook while chunks unload keeps
     * the server from stopping.
     */
    private BlockEntity loaded(BlockPos pos) {
        return level != null && level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
    }

    @Override
    protected void handle(WarehouseEvent event) {
        if (event instanceof WarehouseEvent.Dirty dirty) {
            if (sources.contains(dirty.pos())) {
                markDirty(dirty.pos());
            }
        } else if (event instanceof WarehouseEvent.OutputChanged output) {
            retryHeld(output.pos());
        } else if (event instanceof WarehouseEvent.Intake intake) {
            if (stations.contains(intake.pos())) {
                pendingStations.add(intake.pos());
            }
        } else if (event instanceof WarehouseEvent.LiftUpgradesChanged lift) {
            if (elevators.contains(Conversions.toNodePos(lift.pos()))) {
                shaftsDirty = true;
            }
        } else if (event instanceof WarehouseEvent.ZoneChanged zoned) {
            if (loaded(zoned.pos()) instanceof StorageNodeBlockEntity node && node.holdsStock()) {
                zones.set(zoned.pos(), node.zoneDefinition());
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
        if (level == null) {
            return;
        }
        if (lampsDirty) {
            pushLamps();
        }
        if (controllers == 1) {
            tickController();
        }
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
        rules.tick(level);
        if (++fieldTimer >= StorageStatsManager.INSTANCE.fieldCheckInterval()) {
            fieldTimer = 0;
            tickFields();
        }
        if (!sources.isEmpty()) {
            pollSources();
        }
    }

    /** The span, site record, power and stock snapshot of the single controller. */
    private void tickController() {
        if (spanDirty && controllerPos != null) {
            updateSpan();
        }
        if (wirelessDirty) {
            controller().ifPresent(controller -> {
                wirelessDirty = false;
                SiteRegistry.of(level).updateWireless(controller, controller.hasWirelessUpgrade(),
                        controller.poweredForWireless(), !packingStations().isEmpty());
            });
        }
        controller().ifPresent(WarehouseControllerBlockEntity::tickPower);
        if (++snapshotTimer >= StorageStatsManager.INSTANCE.siteSnapshotInterval()) {
            snapshotTimer = 0;
            snapshotStock();
            controller().ifPresent(controller -> SiteRegistry.of(level).updateEnergy(controller));
        }
    }

    /** Polls dirty sources first, then walks the rest round robin, within the per tick budget. */
    private void pollSources() {
        int budget = StorageStatsManager.INSTANCE.sourcesPerTick();
        while (budget > 0 && !dirtyQueue.isEmpty()) {
            BlockPos pos = dirtyQueue.poll();
            dirtySet.remove(pos);
            if (sources.contains(pos)) {
                poll(pos);
                budget--;
            }
        }
        if (pollOrderStale) {
            pollOrder.clear();
            pollOrder.addAll(sources);
            pollOrderStale = false;
        }
        for (int i = 0; i < budget && !pollOrder.isEmpty(); i++) {
            cursor = (cursor + 1) % pollOrder.size();
            BlockPos pos = pollOrder.get(cursor);
            if (sources.contains(pos)) {
                poll(pos);
            }
        }
    }

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
            Optional<BlockPos> target = firstTarget(key, tiers,
                    candidate -> (!isRack(candidate) || fleet.canCarry(pos, candidate)) && hasRoom(candidate, stack));
            if (target.isEmpty()) {
                if (blockedKey == null) {
                    blockedKey = key;
                    blockedZone = zones.matchedZone(tiers);
                    blockedByRack = firstTarget(key, tiers, rack -> isRack(rack) && hasRoom(rack, stack)).isPresent();
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
        return firstTarget(key, tiers(key), pos -> handlerOf(pos) != null).isPresent();
    }

    private boolean hasRoom(BlockPos pos, ItemStack stack) {
        ItemView handler = handlerOf(pos);
        return handler != null && handler.insertItem(stack, true).getCount() < stack.getCount();
    }

    @Override
    public Optional<BlockPos> putAwayTarget(ItemStack stack, Predicate<BlockPos> allowed) {
        ItemKey key = ItemKey.of(stack);
        return firstTarget(key, tiers(key), pos -> allowed.test(pos) && hasRoom(pos, stack));
    }

    /**
     * Queues a supply run for each Field Head on this warehouse's rails whose
     * buffer runs low, and a field run for each that has work, when a
     * matching shuttle can take it. Each head reads its produce stock first.
     * Polls the loaded heads only.
     */
    private void tickFields() {
        if (status() != Status.ONLINE) {
            return;
        }
        boolean harvesters = fleet.hasFieldShuttles(JobNeeds.HARVESTER);
        boolean spreaders = fleet.hasFieldShuttles(JobNeeds.SPREADER);
        boolean suppliers = fleet.hasShuttles();
        for (FieldHeadBlockEntity head : FieldHeads.heads(level)) {
            if (head.status() != FieldStatus.SERVING || head.rails().isEmpty()
                    || !members().contains(Conversions.toNodePos(head.rails().iterator().next()))) {
                continue;
            }
            head.readProduceStock(this);
            BlockPos pos = head.getBlockPos();
            if (suppliers && !fleet.hasSupplyJob(pos) && supplyDue(head)) {
                fleet.requestSupply(new SupplyJob(pos));
            }
            if (harvesters && !fleet.hasFieldJob(pos) && fleet.fieldReachable(pos, JobNeeds.HARVESTER)) {
                boolean seeds = head.supplyCount(SupplyBuffer.Part.SEEDS) > 0;
                if (FieldDue.due(head.harvestDue(), head.plantMode(), head.hydrated(), head.tillableCount(), head.plantableCount(), seeds)) {
                    fleet.requestField(new FieldJob(pos));
                }
            }
            if (spreaders && head.fertilizing() && !fleet.hasFertilizeJob(pos) && fleet.fieldReachable(pos, JobNeeds.SPREADER)) {
                long now = level.getGameTime();
                if (head.fertilizeDue(now)) {
                    fleet.requestFertilize(new FertilizeJob(pos));
                    head.fertilizeCalled(now);
                }
            }
        }
    }

    /** Whether a part of the head's buffer is low while this warehouse holds more of its item than the head keeps in stock. */
    private boolean supplyDue(FieldHeadBlockEntity head) {
        for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
            Item item = head.supplyItem(part);
            if (item == null) {
                continue;
            }
            int reserve = part == SupplyBuffer.Part.FERTILIZER ? head.reserve() : 0;
            if (SupplyPlan.due(new SupplyPlan.Part(head.supplyCount(part), head.supplyLevel(part),
                    available(ItemKey.of(new ItemStack(item))), reserve))) {
                return true;
            }
        }
        return false;
    }

    /** Whether a supply run for that head is queued or under way. */
    public boolean hasSupplyJob(BlockPos head) {
        return fleet.hasSupplyJob(head);
    }

    /** Whether a shuttle without a tool can drive to the farm rail beside that head, lifts allowed. */
    public boolean supplyReachable(BlockPos head) {
        return fleet.supplyReachable(head);
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
        ItemView handler = handlerOf(pos);
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
            result = combine(result, taken);
            wanted -= taken.getCount();
        }
        return result;
    }

    /**
     * Hands the controller the chunks its site spans: every member of this
     * network, merged with the known chunks that are not loaded. Looks at no block.
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
            SiteRegistry.of(level).updateStock(controller, index.entries(), level.getGameTime());
        });
    }

    /** The controller's Wireless Upgrade or powered state changed: the site record learns it next tick. */
    public void wirelessChanged() {
        wirelessDirty = true;
    }

    /**
     * Whether the site's own work runs: shuttles out or jobs waiting, inbound
     * buffers to drain, deliveries held. A woken site stays loaded while this is true.
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
            if (state.hasProperty(StorageNodeBlocks.ONLINE) && state.getValue(StorageNodeBlocks.ONLINE) != online) {
                level.setBlock(pos, state.setValue(StorageNodeBlocks.ONLINE, online), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private void poll(BlockPos pos) {
        ItemView handler = handlerOf(pos);
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
                + " sources, " + stations.size() + " stations, " + garages.size() + " garages, " + index.typeCount() + " item types, " + index.totalItems() + " items, " + fleet.summary()
                + (status() == Status.ONLINE && noPower() ? ", no power" : ""));
    }

    /**
     * Takes up to {@code amount} (at most one stack) of the key straight out of
     * the racks, for the op command {@code /servus_warehouse take} only.
     */
    public ItemStack takeDirect(ItemKey key, int amount) {
        ItemStack result = ItemStack.EMPTY;
        int wanted = Math.min(amount, key.stack(1).getMaxStackSize());
        for (BlockPos pos : List.copyOf(index.sourcesHolding(key))) {
            if (wanted <= 0) {
                break;
            }
            ItemStack taken = extractFrom(pos, key, wanted);
            result = combine(result, taken);
            wanted -= taken.getCount();
        }
        return result;
    }
}
