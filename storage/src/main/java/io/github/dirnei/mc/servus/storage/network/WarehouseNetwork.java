package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.lib.network.core.NetworkSet;
import io.github.dirnei.mc.servus.lib.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.index.CandidateNeighbours;
import io.github.dirnei.mc.servus.storage.index.WarehouseIndex;
import io.github.dirnei.mc.servus.storage.shuttle.PickJob;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleFleet;
import io.github.dirnei.mc.servus.storage.station.DispatchStationBlockEntity;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan;
import io.github.dirnei.mc.servus.storage.zone.ZoneRules;
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
    private static boolean railSideAccepted(StorageNodeBlockEntity from, io.github.dirnei.mc.servus.lib.graph.Side side,
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

    private static io.github.dirnei.mc.servus.lib.graph.Side opposite(io.github.dirnei.mc.servus.lib.graph.Side side) {
        return switch (side) {
            case UP -> io.github.dirnei.mc.servus.lib.graph.Side.DOWN;
            case DOWN -> io.github.dirnei.mc.servus.lib.graph.Side.UP;
            case NORTH -> io.github.dirnei.mc.servus.lib.graph.Side.SOUTH;
            case SOUTH -> io.github.dirnei.mc.servus.lib.graph.Side.NORTH;
            case WEST -> io.github.dirnei.mc.servus.lib.graph.Side.EAST;
            case EAST -> io.github.dirnei.mc.servus.lib.graph.Side.WEST;
        };
    }

    private record Source(BlockPos pos, boolean node, BlockCapabilityCache<IItemHandler, Direction> cache) {
    }

    private final Map<BlockPos, Source> sources = new LinkedHashMap<>();
    /** Members that hold no stock, such as rails. Never sources, never surrounded by candidates. */
    private final Set<BlockPos> connectors = new HashSet<>();
    /** Zone key per zoned stock node. Unzoned nodes and foreign inventories are absent. */
    private final Map<BlockPos, String> zoneOf = new HashMap<>();
    /** Display spelling per zone key, as seen on the blocks. */
    private final Map<String, String> zoneDisplay = new HashMap<>();
    /** Set when a zone or rule changed; stations with waiting stacks retry on the next tick. */
    private boolean zonesDirty;
    /** Dispatch stations: members that are neither sources nor travel nodes. */
    private final Set<BlockPos> stations = new HashSet<>();
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
        } else {
            dropSource(blockPos);
            connectors.add(blockPos);
        }
        if (node instanceof WarehouseControllerBlockEntity controller) {
            controllers++;
            fleet.adopt(blockPos, controller.shuttleCount());
        }
        lampsDirty = true;
        fleet.refreshHome();
    }

    @Override
    protected void onNodeRemoved(NodePos pos, StorageNodeBlockEntity node) {
        BlockPos blockPos = Conversions.toBlockPos(pos);
        connectors.remove(blockPos);
        stations.remove(blockPos);
        pendingStations.remove(blockPos);
        blockedStations.remove(blockPos);
        zoneOf.remove(blockPos);
        dropSource(blockPos);
        if (node instanceof WarehouseControllerBlockEntity && blockPos.equals(fleet.controller())) {
            controllers--;
            fleet.release();
        } else if (node instanceof WarehouseControllerBlockEntity) {
            controllers--;
        }
        lampsDirty = true;
        rebuildCandidates();
        fleet.refreshHome();
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
        connectors.addAll(that.connectors);
        stations.addAll(that.stations);
        pendingStations.addAll(that.stations);
        zoneOf.putAll(that.zoneOf);
        zoneDisplay.putAll(that.zoneDisplay);
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

    /** The controller's shuttle count changed. */
    public void fleetChanged(WarehouseControllerBlockEntity controller) {
        fleet.adopt(controller.getBlockPos(), controller.shuttleCount());
    }

    // Zones and rules

    private void rememberZone(BlockPos pos, StorageNodeBlockEntity node) {
        String key = node.zoneKey();
        if (key.isEmpty()) {
            zoneOf.remove(pos);
        } else {
            zoneOf.put(pos, key);
            zoneDisplay.put(key, node.zone());
        }
    }

    /** The rules of this warehouse, present while a controller is in it and loaded. */
    public Optional<ZoneRules> rules() {
        BlockPos controller = fleet.controller();
        if (controller == null || level == null || !level.isLoaded(controller)) {
            return Optional.empty();
        }
        return level.getBlockEntity(controller) instanceof WarehouseControllerBlockEntity be
                ? Optional.of(be.rules()) : Optional.empty();
    }

    private Map<String, PutAwayPlan.Rule<ItemKey>> rulesSnapshot() {
        return rules().map(ZoneRules::snapshot).orElse(Map.of());
    }

    /** The controller's rules were edited. */
    public void rulesChanged() {
        zonesDirty = true;
    }

    /** Tag ids of an item, as the pure plan sees them. */
    public static Set<String> tagsOf(ItemKey key) {
        return key.stack(1).getTags().map(tag -> tag.location().toString()).collect(Collectors.toSet());
    }

    /** Where a stack of this key is tried, in order. */
    public PutAwayPlan.Route route(ItemKey key) {
        return PutAwayPlan.route(key, rulesSnapshot(), WarehouseNetwork::tagsOf);
    }

    /** Stock blocks per zone key, sorted by key. Zones that only have a rule show zero. */
    public Map<String, Integer> zoneBlockCounts() {
        Map<String, Integer> counts = new TreeMap<>();
        for (String key : zoneOf.values()) {
            counts.merge(key, 1, Integer::sum);
        }
        rules().ifPresent(rules -> rules.keys().forEach(key -> counts.putIfAbsent(key, 0)));
        return counts;
    }

    /** The spelling of a zone as stamped on its blocks, or the rule's spelling, or the key. */
    public String zoneDisplay(String key) {
        String display = zoneDisplay.get(key);
        if (display != null) {
            return display;
        }
        return rules().map(rules -> rules.get(key)).map(ZoneRules.Entry::display).orElse(key);
    }

    /** Whether a source may take a stack routed to the pool. */
    private boolean inPool(BlockPos pos, PutAwayPlan.Pool pool, Map<String, PutAwayPlan.Rule<ItemKey>> rules) {
        String zone = zoneOf.get(pos);
        if (pool instanceof PutAwayPlan.Zone target) {
            return target.key().equals(zone);
        }
        if (zone == null) {
            return true;
        }
        PutAwayPlan.Rule<ItemKey> rule = rules.get(zone);
        return rule == null || rule.general();
    }

    /**
     * The sources of a pool in insert order: holding the key, empty crates,
     * other storage nodes, foreign inventories already in use.
     */
    private List<BlockPos> targets(PutAwayPlan.Pool pool, ItemKey key) {
        Map<String, PutAwayPlan.Rule<ItemKey>> rules = rulesSnapshot();
        List<BlockPos> holding = new ArrayList<>();
        for (BlockPos pos : index.sourcesHolding(key)) {
            if (sources.containsKey(pos) && inPool(pos, pool, rules)) {
                holding.add(pos);
            }
        }
        Set<BlockPos> holders = new HashSet<>(index.sourcesHolding(key));
        List<BlockPos> emptyCrates = new ArrayList<>();
        List<BlockPos> otherNodes = new ArrayList<>();
        List<BlockPos> foreignInUse = new ArrayList<>();
        for (Source source : sources.values()) {
            if (holders.contains(source.pos()) || !inPool(source.pos(), pool, rules)) {
                continue;
            }
            if (source.node()) {
                if (level.getBlockEntity(source.pos()) instanceof CrateBlockEntity crate && crate.isEmpty()) {
                    emptyCrates.add(source.pos());
                } else {
                    otherNodes.add(source.pos());
                }
            } else if (!index.isEmptySource(source.pos())) {
                foreignInUse.add(source.pos());
            }
        }
        List<BlockPos> result = new ArrayList<>(holding);
        result.addAll(emptyCrates);
        result.addAll(otherNodes);
        result.addAll(foreignInUse);
        return result;
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

    /** Whether a pull of this key rides a shuttle. */
    public boolean canShuttle(ItemKey key) {
        return status() == Status.ONLINE && fleet.canShuttle(key);
    }

    public void requestPick(PickJob job) {
        fleet.requestPick(job);
    }

    public List<io.github.dirnei.mc.servus.storage.shuttle.ShuttleView> shuttleViews() {
        return fleet.views();
    }

    @Override
    protected void onDisposed() {
        sources.clear();
        connectors.clear();
        stations.clear();
        pendingStations.clear();
        blockedStations.clear();
        zoneOf.clear();
        zoneDisplay.clear();
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
            if (connectors.contains(neighbour) || stations.contains(neighbour)) {
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
            fleet.tick();
            tickStations();
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

    // Dispatch stations

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
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof DispatchStationBlockEntity station) {
                drain(pos, station);
            }
        }
    }

    /** Moves up to {@code put_away_per_tick} unclaimed stacks out of the station's buffer. */
    private void drain(BlockPos pos, DispatchStationBlockEntity station) {
        int budget = StorageStatsManager.INSTANCE.putAwayPerTick();
        PutAwayPlan.Route blockedRoute = null;
        ItemKey blockedKey = null;
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
            PutAwayPlan.Route route = route(key);
            Optional<BlockPos> target = chooseTarget(route, key, stack);
            if (target.isEmpty()) {
                if (blockedRoute == null) {
                    blockedRoute = route;
                    blockedKey = key;
                }
                continue;
            }
            budget--;
            if (fleet.canCarry(pos, target.get())) {
                station.claim(slot);
                fleet.requestPutAway(new io.github.dirnei.mc.servus.storage.shuttle.PutAwayJob(pos, slot, key, stack.getCount(), target.get()));
            } else {
                ItemStack taken = station.buffer().extractItem(slot, stack.getCount(), false);
                ItemStack left = insertAt(target.get(), taken);
                returnToStation(pos, left);
            }
        }
        if (more) {
            pendingStations.add(pos);
        }
        if (blockedRoute != null) {
            blockedStations.add(pos);
            String zone = blockedRoute.matched() == null ? null : zoneDisplay(blockedRoute.matched());
            station.setBlocked(blockedRoute.blocked(), blockedKey, zone);
        } else {
            blockedStations.remove(pos);
            station.clearBlocked();
        }
    }

    /** The first block of the route that takes at least one item of the stack. */
    public Optional<BlockPos> chooseTarget(PutAwayPlan.Route route, ItemKey key, ItemStack stack) {
        for (PutAwayPlan.Pool pool : route.pools()) {
            for (BlockPos pos : targets(pool, key)) {
                Source source = sources.get(pos);
                IItemHandler handler = source == null ? null : source.cache().getCapability();
                if (handler != null && ItemHandlerHelper.insertItem(handler, stack, true).getCount() < stack.getCount()) {
                    return Optional.of(pos);
                }
            }
        }
        return Optional.empty();
    }

    /** Inserts into exactly that source; returns what did not fit. */
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
    public void returnToStation(BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack left = stack;
        if (level.getBlockEntity(pos) instanceof DispatchStationBlockEntity station) {
            left = ItemHandlerHelper.insertItem(station.buffer(), stack, false);
        }
        if (!left.isEmpty()) {
            Block.popResource(level, pos, left);
        }
    }

    /** Takes up to amount of the key out of a station's buffer and frees the slot's claim. */
    public ItemStack takeFromStation(BlockPos pos, int slot, ItemKey key, int amount) {
        if (!(level.getBlockEntity(pos) instanceof DispatchStationBlockEntity station)) {
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
                + " sources, " + stations.size() + " stations, " + index.typeCount() + " item types, " + index.totalItems() + " items, " + fleet.summary());
    }

    // Operations

    /** Pulls up to {@code amount} (at most one stack) of the key out of the warehouse. */
    public ItemStack extract(ItemKey key, int amount) {
        ItemStack result = ItemStack.EMPTY;
        int wanted = Math.min(amount, key.stack(1).getMaxStackSize());
        for (BlockPos pos : index.sourcesHolding(key)) {
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
     * Puts a stack into the warehouse by the put-away routing: its zone, else
     * general storage, then the overflow zone; within a pool sources already
     * holding it, then empty crates, then other storage nodes, then foreign
     * inventories in use.
     *
     * @return what did not fit
     */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        ItemKey key = ItemKey.of(stack);
        ItemStack remaining = stack.copy();
        for (PutAwayPlan.Pool pool : route(key).pools()) {
            remaining = insertInto(targets(pool, key), remaining);
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
