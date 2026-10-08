package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.graph.ConnectionRule;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

/**
 * One data network: connected Data Cables plus the blocks they touch that
 * expose the production or stock capabilities. Members are read through
 * capability caches whose invalidation posts {@link DataEvent.SidesChanged},
 * so nothing is scanned per tick. Server thread only.
 */
public final class DataNetwork extends ServusNetwork<DataCableBlockEntity, DataEvent> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "data");

    /** The graph holds cables only; members are kept beside it. */
    public static final NetworkType<DataCableBlockEntity, DataEvent, DataNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), DataNetwork::new);

    /** The capability caches of one cable side. */
    private record Probe(BlockCapabilityCache<?, Direction> production, BlockCapabilityCache<?, Direction> provider,
                         BlockCapabilityCache<?, Direction> sink) {

        Set<DataRole> roles() {
            Set<DataRole> roles = EnumSet.noneOf(DataRole.class);
            if (production.getCapability() != null) {
                roles.add(DataRole.PRODUCER);
            }
            if (provider.getCapability() != null || sink.getCapability() != null) {
                roles.add(DataRole.STOCK);
            }
            return roles;
        }
    }

    private final DataMembers members = new DataMembers();
    private final Map<NodePos, Probe[]> probes = new HashMap<>();
    private ServerLevel level;

    public DataNetwork(int id, NetworkSet<DataCableBlockEntity, DataEvent, ?> owner) {
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

    /** How many cables the network has. */
    public int cables() {
        return size();
    }

    @Override
    protected void handle(DataEvent event) {
        if (event instanceof DataEvent.SidesChanged changed && probes.containsKey(changed.cable())) {
            readSides(changed.cable());
        }
    }

    @Override
    protected void onNodeAdded(NodePos pos, DataCableBlockEntity cable) {
        if (!(cable.getLevel() instanceof ServerLevel server)) {
            return;
        }
        level = server;
        BlockPos blockPos = Conversions.toBlockPos(pos);
        Runnable invalidated = () -> postTo(server, pos);
        Probe[] sides = new Probe[6];
        for (Direction direction : Direction.values()) {
            BlockPos target = blockPos.relative(direction);
            sides[direction.ordinal()] = new Probe(
                    cache(ServusFoundationCapabilities.PRODUCTION, server, target, direction, cable, invalidated),
                    cache(ServusFoundationCapabilities.STOCK_PROVIDER, server, target, direction, cable, invalidated),
                    cache(ServusFoundationCapabilities.STOCK_SINK, server, target, direction, cable, invalidated));
        }
        probes.put(pos, sides);
        // Neighbours are read on the next tick, never while a chunk is loading.
        post(new DataEvent.SidesChanged(pos));
    }

    @Override
    protected void onNodeRemoved(NodePos pos, DataCableBlockEntity cable) {
        probes.remove(pos);
        members.removeCable(pos);
    }

    @Override
    protected void onAbsorb(ServusNetwork<DataCableBlockEntity, DataEvent> other) {
        if (other instanceof DataNetwork that) {
            members.absorb(that.members);
            probes.putAll(that.probes);
            if (level == null) {
                level = that.level;
            }
            // The other mailbox is cleared on disposal, so pending reads are queued here again.
            that.probes.keySet().forEach(cable -> post(new DataEvent.SidesChanged(cable)));
        }
    }

    private void readSides(NodePos pos) {
        Probe[] sides = probes.get(pos);
        for (Side side : Side.ALL) {
            NodePos target = pos.relative(side);
            BlockPos targetPos = Conversions.toBlockPos(target);
            boolean member = level != null && level.isLoaded(targetPos)
                    && !(level.getBlockEntity(targetPos) instanceof DataCableBlockEntity);
            members.touch(pos, side, target, member ? sides[side.ordinal()].roles() : Set.of());
        }
    }

    private static <T> BlockCapabilityCache<T, Direction> cache(BlockCapability<T, Direction> capability, ServerLevel level,
                                                                BlockPos target, Direction direction,
                                                                DataCableBlockEntity cable, Runnable invalidated) {
        return BlockCapabilityCache.create(capability, level, target, direction.getOpposite(),
                () -> !cable.isRemoved(), invalidated);
    }

    /** Invalidation must not query, so it only queues a read for whichever network owns the cable now. */
    private static void postTo(ServerLevel level, NodePos cable) {
        NetworkManager.of(level).set(TYPE).networkAt(cable)
                .ifPresent(network -> network.post(new DataEvent.SidesChanged(cable)));
    }
}
