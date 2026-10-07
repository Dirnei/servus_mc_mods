package io.github.dirnei.mc.servus.logistics.tank;

import io.github.dirnei.mc.servus.lib.graph.ConnectionRule;
import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.lib.network.core.NetworkSet;
import io.github.dirnei.mc.servus.lib.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * A connected group of Fluid Tank blocks. Every block stores its own share of
 * the fluid; the network only lays the shares out. When the group fills its
 * bounding box it is formed: fill and drain act on the summed content, which
 * is laid out again over the blocks, lowest layer first, before anyone reads a
 * share and at the end of the tick. An unformed group leaves every block on
 * its own share.
 *
 * <p>Server thread only. No work on idle tanks: a tick only does something
 * after a fill, a drain or a membership change.
 */
public final class TankNetwork extends ServusNetwork<FluidTankBlockEntity, Void> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusLogisticsIds.MOD_ID, "fluid_tank");

    public static final NetworkType<FluidTankBlockEntity, Void, TankNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), TankNetwork::new);

    /** Minimum ticks between two client updates of one block. */
    public static final int SYNC_INTERVAL = 10;

    private final Map<NodePos, FluidTankBlockEntity> nodes = new LinkedHashMap<>();
    private final Set<FluidTankBlockEntity> waitingSync = new LinkedHashSet<>();
    private ServerLevel level;

    private boolean shapeDirty = true;
    private TankShape shape;
    private boolean formed;
    /** The formed tank's content. Unused while unformed. */
    private FluidStack total = FluidStack.EMPTY;
    /** The total changed and the shares do not show it yet. Only ever true while formed. */
    private boolean layoutPending;
    private boolean statesDirty;

    public TankNetwork(int id, NetworkSet<FluidTankBlockEntity, Void, ?> owner) {
        super(id, owner);
    }

    @Override
    protected void handle(Void event) {
    }

    // Membership

    @Override
    protected void onNodeAdded(NodePos pos, FluidTankBlockEntity tank) {
        if (tank.getLevel() instanceof ServerLevel server) {
            level = server;
        }
        flush();
        nodes.put(pos, tank);
        topologyChanged();
    }

    @Override
    protected void onNodeRemoved(NodePos pos, FluidTankBlockEntity tank) {
        flush();
        nodes.remove(pos);
        waitingSync.remove(tank);
        topologyChanged();
    }

    @Override
    protected void onAbsorb(ServusNetwork<FluidTankBlockEntity, Void> other) {
        if (other instanceof TankNetwork that) {
            that.flush();
            flush();
            nodes.putAll(that.nodes);
            waitingSync.addAll(that.waitingSync);
            if (level == null) {
                level = that.level;
            }
            topologyChanged();
        }
    }

    @Override
    protected void onDisposed() {
        nodes.clear();
        waitingSync.clear();
    }

    private void topologyChanged() {
        shapeDirty = true;
        statesDirty = true;
    }

    // Shape

    /** Decides whether the group is formed, after a membership or share change. */
    private void ensureShape() {
        if (!shapeDirty) {
            return;
        }
        shapeDirty = false;
        layoutPending = false;
        if (nodes.isEmpty()) {
            shape = null;
            formed = false;
            total = FluidStack.EMPTY;
            return;
        }
        shape = TankShape.of(nodes.keySet());
        boolean wasFormed = formed;
        FluidStack sum = FluidStack.EMPTY;
        boolean oneFluid = true;
        for (FluidTankBlockEntity tank : nodes.values()) {
            FluidStack share = tank.rawShare();
            if (share.isEmpty()) {
                continue;
            }
            if (sum.isEmpty()) {
                sum = share.copy();
            } else if (FluidStack.isSameFluidSameComponents(sum, share)) {
                sum.grow(share.getAmount());
            } else {
                oneFluid = false;
                break;
            }
        }
        formed = nodes.size() > 1 && shape.isBox() && oneFluid;
        total = formed ? sum : FluidStack.EMPTY;
        layoutPending = formed;
        if (formed != wasFormed) {
            statesDirty = true;
        }
    }

    /** True when the group fills its bounding box and acts as one tank. */
    public boolean isFormed() {
        ensureShape();
        return formed;
    }

    /** Blocks in the group. */
    public int blockCount() {
        return nodes.size();
    }

    /** Writes the formed content into the blocks' shares when it changed. */
    public void flush() {
        if (!layoutPending) {
            return;
        }
        layoutPending = false;
        if (!formed || shape == null) {
            return;
        }
        TankLayers layers = TankLayers.of(shape.blocksPerLayer(), shape.height(), capacityPerBlock(), total.getAmount());
        if (layers.total() != total.getAmount()) {
            total = layers.total() == 0 ? FluidStack.EMPTY : total.copyWithAmount((int) layers.total());
        }
        for (Map.Entry<NodePos, FluidTankBlockEntity> entry : nodes.entrySet()) {
            NodePos pos = entry.getKey();
            int amount = layers.amount(shape.layer(pos), shape.indexInLayer(pos));
            FluidStack share = amount == 0 ? FluidStack.EMPTY : total.copyWithAmount(amount);
            FluidTankBlockEntity tank = entry.getValue();
            if (!FluidStack.matches(share, tank.rawShare())) {
                tank.setShareFromNetwork(share);
                waitingSync.add(tank);
            }
        }
    }

    // Content of a formed tank

    private static int capacityPerBlock() {
        return TankStatsManager.INSTANCE.capacityPerBlock();
    }

    /** Capacity of the formed tank in mB. */
    public int capacity() {
        return (int) Math.min(Integer.MAX_VALUE, (long) nodes.size() * capacityPerBlock());
    }

    /** A copy of the formed tank's content. */
    public FluidStack content() {
        ensureShape();
        return total.copy();
    }

    /**
     * The one fluid this group holds or may hold, empty when every block is
     * empty. For an unformed group, the first non empty share.
     */
    public FluidStack groupFluid() {
        ensureShape();
        if (formed) {
            return total;
        }
        for (FluidTankBlockEntity tank : nodes.values()) {
            if (!tank.rawShare().isEmpty()) {
                return tank.rawShare();
            }
        }
        return FluidStack.EMPTY;
    }

    public int fill(FluidStack resource, FluidAction action) {
        ensureShape();
        if (resource.isEmpty() || (!total.isEmpty() && !FluidStack.isSameFluidSameComponents(total, resource))) {
            return 0;
        }
        int filled = Math.min(capacity() - total.getAmount(), resource.getAmount());
        if (filled <= 0) {
            return 0;
        }
        if (action.execute()) {
            if (total.isEmpty()) {
                total = resource.copyWithAmount(filled);
            } else {
                total.grow(filled);
            }
            layoutPending = true;
        }
        return filled;
    }

    public FluidStack drain(int maxDrain, FluidAction action) {
        ensureShape();
        int drained = Math.min(maxDrain, total.getAmount());
        if (drained <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack result = total.copyWithAmount(drained);
        if (action.execute()) {
            total.shrink(drained);
            if (total.isEmpty()) {
                total = FluidStack.EMPTY;
            }
            layoutPending = true;
        }
        return result;
    }

    public FluidStack drain(FluidStack resource, FluidAction action) {
        ensureShape();
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(total, resource)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    /** A block's share changed outside the network: re-sum on the next read. */
    void shareChangedOutside(FluidTankBlockEntity tank) {
        flush();
        shapeDirty = true;
        waitingSync.add(tank);
    }

    /** Queues a throttled client update of one block. */
    void requestSync(FluidTankBlockEntity tank) {
        waitingSync.add(tank);
    }

    // Tick

    @Override
    protected void onTick() {
        if (level == null) {
            return;
        }
        ensureShape();
        flush();
        if (statesDirty) {
            pushStates();
        }
        if (!waitingSync.isEmpty()) {
            long now = level.getGameTime();
            Iterator<FluidTankBlockEntity> it = waitingSync.iterator();
            while (it.hasNext()) {
                FluidTankBlockEntity tank = it.next();
                if (tank.isRemoved()) {
                    it.remove();
                } else if (tank.trySync(now, SYNC_INTERVAL)) {
                    it.remove();
                }
            }
        }
    }

    /** Sets the six joined flags of every loaded member: joined toward members of the same formed tank. */
    private void pushStates() {
        statesDirty = false;
        for (NodePos member : nodes.keySet()) {
            BlockPos pos = Conversions.toBlockPos(member);
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof FluidTankBlock)) {
                continue;
            }
            BlockState updated = state;
            for (Direction direction : Direction.values()) {
                boolean joined = formed && nodes.containsKey(Conversions.toNodePos(pos.relative(direction)));
                updated = updated.setValue(FluidTankBlock.property(direction), joined);
            }
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }
}
