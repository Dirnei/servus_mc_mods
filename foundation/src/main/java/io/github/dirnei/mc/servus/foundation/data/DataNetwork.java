package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.foundation.production.ItemSupplier;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.foundation.data.packet.DeliveryReason;
import io.github.dirnei.mc.servus.foundation.data.packet.PacketLimitsManager;
import io.github.dirnei.mc.servus.foundation.data.packet.PacketRoutes;
import io.github.dirnei.mc.servus.foundation.data.packet.Shipment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

/**
 * One data network: connected data nodes (ducts carrying the data line and
 * carriers such as terminals) plus the blocks the ducts touch that expose the
 * production or stock capabilities. A carrier is a member itself.
 * Members are read through capability caches whose invalidation posts
 * {@link DataEvent.SidesChanged}, so nothing is scanned per tick. Server
 * thread only.
 */
public final class DataNetwork extends ServusNetwork<DataNode, DataEvent> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "data");

    /** Ticks between upkeep checks of the members. */
    public static final int UPKEEP_INTERVAL = 40;
    /** Ticks after which an incomplete upkeep request is given up. */
    public static final int UPKEEP_TIMEOUT = 1200;

    /** The graph holds data nodes only and links them through open sides; members are kept beside it. */
    public static final NetworkType<DataNode, DataEvent, DataNetwork> TYPE = new NetworkType<>(ID,
            (from, fromNode, side, to, toNode) -> fromNode.dataSideOpen(Conversions.toDirection(side)), DataNetwork::new);

    /** The capability caches of one node side. */
    private record Probe(BlockCapabilityCache<?, Direction> production, BlockCapabilityCache<?, Direction> provider,
                         BlockCapabilityCache<?, Direction> sink, BlockCapabilityCache<?, Direction> link,
                         BlockCapabilityCache<?, Direction> upkeep) {

        Set<DataRole> roles() {
            Set<DataRole> roles = EnumSet.noneOf(DataRole.class);
            if (production.getCapability() != null) {
                roles.add(DataRole.PRODUCER);
            }
            if (provider.getCapability() != null || sink.getCapability() != null) {
                roles.add(DataRole.STOCK);
            }
            if (link.getCapability() != null) {
                roles.add(DataRole.LINK);
            }
            if (upkeep.getCapability() != null) {
                roles.add(DataRole.UPKEEP);
            }
            return roles;
        }
    }

    private final DataMembers members = new DataMembers();
    private final Map<NodePos, Probe[]> probes = new HashMap<>();
    private final Map<NodePos, Probe> selfProbes = new HashMap<>();
    private final List<Shipment> shipments = new ArrayList<>();
    private final UpkeepBook<BlockPos> upkeep = new UpkeepBook<>();
    private int upkeepTimer;
    private ServerLevel level;

    public DataNetwork(int id, NetworkSet<DataNode, DataEvent, ?> owner) {
        super(id, owner);
    }

    /** Every member with its roles. */
    public List<DataMember> dataMembers() {
        List<DataMember> result = new ArrayList<>();
        members.all().forEach((pos, roles) -> result.add(new DataMember(Conversions.toBlockPos(pos), roles)));
        return Collections.unmodifiableList(result);
    }

    /** The members that have the role. */
    public List<DataMember> dataMembers(DataRole role) {
        return dataMembers().stream().filter(member -> member.has(role)).toList();
    }

    public boolean hasMember(BlockPos pos) {
        return members.contains(Conversions.toNodePos(pos));
    }

    /** How many cables (ducts carrying the data line) the network has; carriers do not count. */
    public int cables() {
        int count = 0;
        for (NodePos pos : members()) {
            if (owner().graph().node(pos).map(DataNode::dataCable).orElse(false)) {
                count++;
            }
        }
        return count;
    }

    public ServerLevel level() {
        return level;
    }

    /** Whether {@code pos} is a data node of this network or one of its members. */
    public boolean reaches(BlockPos pos) {
        NodePos node = Conversions.toNodePos(pos);
        return owns(node) || members.contains(node);
    }

    /** Running shipments, read only. */
    public List<Shipment> shipments() {
        return Collections.unmodifiableList(shipments);
    }

    /** Starts ticking a shipment; see {@code DataPackets}. */
    public void addShipment(Shipment shipment) {
        shipments.add(shipment);
    }

    /**
     * The shortest route from {@code from} to {@code to}: the source position
     * when it is not a data node, the data nodes, and the target position when
     * it is not a data node. Deliveries are instant; the route only tells
     * whether both ends are linked and which faces touch the line.
     */
    public Optional<List<NodePos>> packetRoute(BlockPos from, BlockPos to) {
        NodePos source = Conversions.toNodePos(from);
        NodePos target = Conversions.toNodePos(to);
        return route(source, endpoints(source), target);
    }

    private Optional<List<NodePos>> route(NodePos source, Set<NodePos> starts, NodePos target) {
        if (source.equals(target)) {
            return Optional.empty();
        }
        return PacketRoutes.shortest(starts, endpoints(target), owner().graph()::neighbors).map(path -> {
            List<NodePos> route = new ArrayList<>();
            if (!path.get(0).equals(source)) {
                route.add(source);
            }
            route.addAll(path);
            if (!path.get(path.size() - 1).equals(target)) {
                route.add(target);
            }
            return route;
        }).filter(route -> route.size() >= 2);
    }

    /** The data nodes of this network items can enter or leave {@code pos} through. */
    private Set<NodePos> endpoints(NodePos pos) {
        Set<NodePos> result = new HashSet<>();
        if (owns(pos)) {
            result.add(pos);
            return result;
        }
        for (Side side : Side.ALL) {
            NodePos next = pos.relative(side);
            if (owns(next) && owner().graph().node(next)
                    .map(node -> node.dataSideOpen(Conversions.toDirection(side.opposite()))).orElse(false)) {
                result.add(next);
            }
        }
        return result;
    }

    private boolean owns(NodePos pos) {
        return owner().networkAt(pos).orElse(null) == this;
    }

    @Override
    protected void onTick() {
        if (level == null) {
            return;
        }
        if (!shipments.isEmpty()) {
            var limits = PacketLimitsManager.INSTANCE.limits();
            shipments.removeIf(shipment -> shipment.tick(this, level, limits));
        }
        if (++upkeepTimer >= UPKEEP_INTERVAL) {
            upkeepTimer = 0;
            keepUpkeep(level);
        }
    }

    /** Asks suppliers on the line for the upkeep each upkeep member is missing, one open request per member. */
    private void keepUpkeep(ServerLevel server) {
        List<DataMember> holders = dataMembers(DataRole.UPKEEP);
        upkeep.retain(holders.stream().map(DataMember::pos).toList());
        if (holders.isEmpty()) {
            return;
        }
        List<DataMember> suppliers = null;
        long now = server.getGameTime();
        for (DataMember member : holders) {
            if (!server.isLoaded(member.pos())) {
                continue;
            }
            UpkeepHolder holder = server.getCapability(ServusFoundationCapabilities.UPKEEP, member.pos(), null);
            if (holder == null || holder.upkeepItem().isEmpty() || holder.upkeepLevel() <= 0) {
                continue;
            }
            int count = holder.upkeepCount();
            int missing = upkeep.ask(member.pos(), count, holder.upkeepLevel(), now);
            if (missing <= 0) {
                continue;
            }
            if (suppliers == null) {
                suppliers = new ArrayList<>(dataMembers());
                suppliers.sort(Comparator.comparingLong(candidate -> candidate.pos().asLong()));
            }
            for (DataMember candidate : suppliers) {
                if (candidate.pos().equals(member.pos()) || !server.isLoaded(candidate.pos())) {
                    continue;
                }
                ItemSupplier supplier = server.getCapability(ServusFoundationCapabilities.SUPPLIER, candidate.pos(), null);
                if (supplier == null) {
                    continue;
                }
                int answer = supplier.supply(member.pos(), holder.upkeepItem().copyWithCount(1), missing);
                if (answer > 0) {
                    upkeep.asked(member.pos(), answer, count, now, UPKEEP_TIMEOUT);
                    holder.upkeepRequested(answer, now + UPKEEP_TIMEOUT);
                    break;
                }
            }
        }
    }

    /** Puts every shipment's items somewhere safe; used when the network goes away or the server stops. */
    public void settleAll() {
        if (level != null) {
            for (Shipment shipment : new ArrayList<>(shipments)) {
                shipment.settle(level, DeliveryReason.ROUTE_BROKEN);
            }
        }
        shipments.clear();
    }

    @Override
    protected void onDisposed() {
        settleAll();
    }

    @Override
    protected void handle(DataEvent event) {
        if (event instanceof DataEvent.SidesChanged changed) {
            if (probes.containsKey(changed.cable())) {
                readSides(changed.cable());
            }
            if (selfProbes.containsKey(changed.cable())) {
                readSelf(changed.cable());
            }
        }
    }

    @Override
    protected void onNodeAdded(NodePos pos, DataNode node) {
        if (!(node.dataLevel() instanceof ServerLevel server)) {
            return;
        }
        level = server;
        BlockPos blockPos = Conversions.toBlockPos(pos);
        Runnable invalidated = () -> postTo(server, pos);
        if (!node.dataCable()) {
            // A carrier is a member itself and never makes its neighbours members.
            selfProbes.put(pos, probe(server, blockPos, null, node, invalidated));
            post(new DataEvent.SidesChanged(pos));
            return;
        }
        Probe[] sides = new Probe[6];
        for (Direction direction : Direction.values()) {
            sides[direction.ordinal()] = probe(server, blockPos.relative(direction), direction.getOpposite(), node, invalidated);
        }
        probes.put(pos, sides);
        // Neighbours are read on the next tick, never while a chunk is loading.
        post(new DataEvent.SidesChanged(pos));
    }

    /** A duct's data line changed its tier in place: running deliveries pick their path again. */
    public void tierChanged(NodePos pos) {
        shipments.forEach(shipment -> shipment.nodeRemoved(pos));
    }

    @Override
    protected void onNodeRemoved(NodePos pos, DataNode node) {
        probes.remove(pos);
        selfProbes.remove(pos);
        members.removeCable(pos);
        shipments.forEach(shipment -> shipment.nodeRemoved(pos));
    }

    @Override
    protected void onAbsorb(ServusNetwork<DataNode, DataEvent> other) {
        if (other instanceof DataNetwork that) {
            members.absorb(that.members);
            probes.putAll(that.probes);
            selfProbes.putAll(that.selfProbes);
            shipments.addAll(that.shipments);
            that.shipments.clear();
            if (level == null) {
                level = that.level;
            }
            // The other mailbox is cleared on disposal, so pending reads are queued here again.
            that.probes.keySet().forEach(cable -> post(new DataEvent.SidesChanged(cable)));
            that.selfProbes.keySet().forEach(carrier -> post(new DataEvent.SidesChanged(carrier)));
        }
    }

    private void readSides(NodePos pos) {
        Probe[] sides = probes.get(pos);
        DataNode node = owner().graph().node(pos).orElse(null);
        for (Side side : Side.ALL) {
            NodePos target = pos.relative(side);
            BlockPos targetPos = Conversions.toBlockPos(target);
            boolean member = level != null && node != null && node.dataSideOpen(Conversions.toDirection(side))
                    && level.isLoaded(targetPos)
                    && !owner().graph().contains(target)
                    && !(level.getBlockEntity(targetPos) instanceof DataNode);
            members.touch(pos, side, target, member ? sides[side.ordinal()].roles() : Set.of());
        }
    }

    /** A carrier's own roles, read from its capabilities without a side. */
    private void readSelf(NodePos pos) {
        Probe probe = selfProbes.get(pos);
        boolean loaded = level != null && level.isLoaded(Conversions.toBlockPos(pos));
        members.self(pos, loaded ? probe.roles() : Set.of());
    }

    /** The capability caches of {@code target} seen from {@code face}, null for a block's own capabilities. */
    private static Probe probe(ServerLevel level, BlockPos target, Direction face, DataNode node, Runnable invalidated) {
        return new Probe(
                cache(ServusFoundationCapabilities.PRODUCTION, level, target, face, node, invalidated),
                cache(ServusFoundationCapabilities.STOCK_PROVIDER, level, target, face, node, invalidated),
                cache(ServusFoundationCapabilities.STOCK_SINK, level, target, face, node, invalidated),
                cache(ServusFoundationCapabilities.DATA_LINK, level, target, face, node, invalidated),
                cache(ServusFoundationCapabilities.UPKEEP, level, target, face, node, invalidated));
    }

    private static <T> BlockCapabilityCache<T, Direction> cache(BlockCapability<T, Direction> capability, ServerLevel level,
                                                                BlockPos target, Direction face,
                                                                DataNode node, Runnable invalidated) {
        return BlockCapabilityCache.create(capability, level, target, face, () -> !node.dataRemoved(), invalidated);
    }

    /** Asks the network that owns the node at {@code pos} to read its sides again on the next tick. */
    public static void sidesChanged(ServerLevel level, BlockPos pos) {
        postTo(level, Conversions.toNodePos(pos));
    }

    /** Invalidation must not query, so it only queues a read for whichever network owns the node now. */
    private static void postTo(ServerLevel level, NodePos node) {
        NetworkManager.of(level).set(TYPE).networkAt(node)
                .ifPresent(network -> network.post(new DataEvent.SidesChanged(node)));
    }
}
