package io.github.dirnei.mc.servus.foundation.logistics.network;

import io.github.dirnei.mc.servus.foundation.graph.ConnectionRule;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * The network of one duct line. Keeps an endpoint per enabled side of every
 * member duct, rebuilt when a duct joins or reports a configuration change.
 *
 * @param <P> the endpoint type
 */
public abstract class LineNetwork<P> extends ServusNetwork<UtilityDuctBlockEntity, DuctEvent> {

    /** Graph rule of the line networks: a solid panel closes the side. */
    static final ConnectionRule<UtilityDuctBlockEntity> RULE =
            (from, fromNode, side, to, toNode) -> UtilityDuctBlockEntity.sideOpen(fromNode, side);

    private final Line line;
    /** Endpoints per member duct position. */
    protected final Map<NodePos, List<P>> endpoints;
    protected ServerLevel level;

    protected LineNetwork(int id, NetworkSet<UtilityDuctBlockEntity, DuctEvent, ?> owner, Line line, Map<NodePos, List<P>> endpoints) {
        super(id, owner);
        this.line = line;
        this.endpoints = endpoints;
    }

    /** The endpoint of one enabled duct side whose neighbour is at {@code target}. */
    protected abstract P endpoint(UtilityDuctBlockEntity duct, Direction side, BlockPos target, SideMode mode, int channel);

    /**
     * How many sides of this network sit on each channel id, counting only
     * sides whose arm of this line ends at a block other than a duct.
     */
    public Map<Integer, Integer> channelUses() {
        Map<Integer, Integer> uses = new HashMap<>();
        for (EndpointTally.Side side : blockSides()) {
            uses.merge(side.channel(), 1, Integer::sum);
        }
        return uses;
    }

    /**
     * The channel and mode of every member duct side whose arm of this line
     * ends at a block other than a duct, read live from the loaded ducts.
     */
    public List<EndpointTally.Side> blockSides() {
        List<EndpointTally.Side> sides = new ArrayList<>();
        if (level == null) {
            return sides;
        }
        for (NodePos pos : endpoints.keySet()) {
            if (!(level.getBlockEntity(Conversions.toBlockPos(pos)) instanceof UtilityDuctBlockEntity duct)) {
                continue;
            }
            for (Direction side : Direction.values()) {
                if (duct.hasArm(line, side) && duct.isEndpoint(side)) {
                    sides.add(new EndpointTally.Side(duct.channel(line, side), duct.mode(line, side)));
                }
            }
        }
        return sides;
    }

    /** Called after endpoints were added, rebuilt or removed. */
    protected void endpointsChanged() {
    }

    @Override
    protected void handle(DuctEvent event) {
        if (event instanceof DuctEvent.ConfigChanged changed && level != null
                && level.getBlockEntity(Conversions.toBlockPos(changed.pos())) instanceof UtilityDuctBlockEntity duct) {
            rebuild(changed.pos(), duct);
        }
    }

    @Override
    protected void onNodeAdded(NodePos pos, UtilityDuctBlockEntity duct) {
        if (!(duct.getLevel() instanceof ServerLevel server)) {
            return;
        }
        level = server;
        rebuild(pos, duct);
    }

    private void rebuild(NodePos pos, UtilityDuctBlockEntity duct) {
        BlockPos blockPos = Conversions.toBlockPos(pos);
        List<P> list = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            SideMode mode = duct.mode(line, direction);
            if (mode != SideMode.DISABLED) {
                list.add(endpoint(duct, direction, blockPos.relative(direction), mode, duct.channel(line, direction)));
            }
        }
        endpoints.put(pos, list);
        endpointsChanged();
    }

    @Override
    protected void onNodeRemoved(NodePos pos, UtilityDuctBlockEntity duct) {
        endpoints.remove(pos);
        endpointsChanged();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onAbsorb(ServusNetwork<UtilityDuctBlockEntity, DuctEvent> other) {
        if (other.getClass() == getClass()) {
            LineNetwork<P> that = (LineNetwork<P>) other;
            endpoints.putAll(that.endpoints);
            if (level == null) {
                level = that.level;
            }
            endpointsChanged();
        }
    }

    @Override
    protected void onDisposed() {
        endpoints.clear();
    }
}
