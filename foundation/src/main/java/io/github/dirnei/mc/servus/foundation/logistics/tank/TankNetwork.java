package io.github.dirnei.mc.servus.foundation.logistics.tank;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.graph.ConnectionRule;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import java.util.HashMap;
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
 * A connected group of Fluid Tank blocks, split by {@link TankBoxes} into
 * tanks that each fill a box. Every block stores its own share of the fluid
 * and the box it belongs to; the network keeps one content per box, lays it
 * out over the box's blocks lowest layer first before anyone reads a share
 * and at the end of the tick. A lone block is a box of one.
 *
 * <p>Server thread only. No work on idle tanks: a tick only does something
 * after a fill, a drain or a membership change, plus a slow recheck while
 * blocks wait for a saved box to load completely.
 */
public final class TankNetwork extends ServusNetwork<FluidTankBlockEntity, Void> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "fluid_tank");

    public static final NetworkType<FluidTankBlockEntity, Void, TankNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), TankNetwork::new);

    /** Minimum ticks between two client updates of one block. */
    public static final int SYNC_INTERVAL = 10;
    /** Ticks between two rechecks of blocks waiting for their saved box. */
    private static final int RECHECK_INTERVAL = 20;

    private final Map<NodePos, FluidTankBlockEntity> nodes = new LinkedHashMap<>();
    private final Set<FluidTankBlockEntity> waitingSync = new LinkedHashSet<>();
    private final TankBoxes boxes = new TankBoxes(new Rules());
    private ServerLevel level;

    /** Content per box, rebuilt from the shares after a membership change. */
    private final Map<TankShape, FluidStack> totals = new HashMap<>();
    /** Boxes whose content changed and whose shares do not show it yet. */
    private final Set<TankShape> layoutPending = new LinkedHashSet<>();
    private boolean totalsDirty = true;
    private boolean statesDirty;
    private int recheckCooldown;

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
        boxes.add(pos, tank.savedBox());
        topologyChanged();
    }

    @Override
    protected void onNodeRemoved(NodePos pos, FluidTankBlockEntity tank) {
        flush();
        nodes.remove(pos);
        waitingSync.remove(tank);
        boxes.remove(pos);
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
            boxes.absorb(that.boxes);
            topologyChanged();
        }
    }

    @Override
    protected void onDisposed() {
        nodes.clear();
        waitingSync.clear();
        totals.clear();
        layoutPending.clear();
    }

    /** Boxes may have changed: content is summed again and every block learns its box right away. */
    private void topologyChanged() {
        totalsDirty = true;
        statesDirty = true;
        for (Map.Entry<NodePos, FluidTankBlockEntity> entry : nodes.entrySet()) {
            if (boxes.isWaiting(entry.getKey())) {
                continue;
            }
            TankShape box = boxes.boxOf(entry.getKey());
            entry.getValue().setSavedBox(box != null && box.volume() > 1 ? box : null);
        }
    }

    private final class Rules implements TankBoxes.Rules {

        @Override
        public boolean mayArrive(NodePos pos) {
            if (level == null) {
                return true;
            }
            BlockPos blockPos = Conversions.toBlockPos(pos);
            return !level.isLoaded(blockPos) || level.getBlockState(blockPos).getBlock() instanceof FluidTankBlock;
        }

        @Override
        public boolean mayMerge(TankShape a, TankShape b) {
            FluidStack first = fluidOf(a);
            FluidStack second = fluidOf(b);
            return first.isEmpty() || second.isEmpty() || FluidStack.isSameFluidSameComponents(first, second);
        }
    }

    /** The fluid in a box's shares, empty when none. */
    private FluidStack fluidOf(TankShape box) {
        for (NodePos pos : box.positions()) {
            FluidTankBlockEntity tank = nodes.get(pos);
            if (tank != null && !tank.rawShare().isEmpty()) {
                return tank.rawShare();
            }
        }
        return FluidStack.EMPTY;
    }

    // Boxes and content

    /** Sums the shares of every box after a membership change. */
    private void ensureTotals() {
        if (!totalsDirty) {
            return;
        }
        totalsDirty = false;
        totals.clear();
        layoutPending.clear();
        for (TankShape box : boxes.boxes()) {
            FluidStack sum = FluidStack.EMPTY;
            for (NodePos pos : box.positions()) {
                FluidTankBlockEntity tank = nodes.get(pos);
                FluidStack share = tank == null ? FluidStack.EMPTY : tank.rawShare();
                if (share.isEmpty()) {
                    continue;
                }
                if (sum.isEmpty()) {
                    sum = share.copy();
                } else if (FluidStack.isSameFluidSameComponents(sum, share)) {
                    sum.grow(share.getAmount());
                }
            }
            totals.put(box, sum);
            if (box.volume() > 1) {
                layoutPending.add(box);
            }
        }
    }

    /** The box of a block, a lone block's own box when it is not in a bigger one. */
    public TankShape boxAt(NodePos pos) {
        TankShape box = boxes.boxOf(pos);
        return box == null ? TankShape.single(pos) : box;
    }

    /** True when the block is part of a tank of more than one block. */
    public boolean isFormed(NodePos pos) {
        return boxAt(pos).volume() > 1;
    }

    /** Blocks in the connected group. */
    public int blockCount() {
        return nodes.size();
    }

    /** Writes every changed box content into its blocks' shares. */
    public void flush() {
        if (layoutPending.isEmpty()) {
            return;
        }
        int capacity = capacityPerBlock();
        for (TankShape box : layoutPending) {
            FluidStack total = totals.getOrDefault(box, FluidStack.EMPTY);
            TankLayers layers = TankLayers.of(box.blocksPerLayer(), box.height(), capacity, total.getAmount());
            if (layers.total() != total.getAmount()) {
                total = layers.total() == 0 ? FluidStack.EMPTY : total.copyWithAmount((int) layers.total());
                totals.put(box, total);
            }
            for (NodePos pos : box.positions()) {
                FluidTankBlockEntity tank = nodes.get(pos);
                if (tank == null) {
                    continue;
                }
                int amount = layers.amount(box.layer(pos), box.indexInLayer(pos));
                FluidStack share = amount == 0 ? FluidStack.EMPTY : total.copyWithAmount(amount);
                if (!FluidStack.matches(share, tank.rawShare())) {
                    tank.setShareFromNetwork(share);
                    waitingSync.add(tank);
                }
            }
        }
        layoutPending.clear();
    }

    private static int capacityPerBlock() {
        return TankStatsManager.INSTANCE.capacityPerBlock();
    }

    /** Capacity in mB of the tank a block belongs to. */
    public int capacity(NodePos pos) {
        return (int) Math.min(Integer.MAX_VALUE, boxAt(pos).volume() * capacityPerBlock());
    }

    /** A copy of the content of the tank a block belongs to. */
    public FluidStack content(NodePos pos) {
        ensureTotals();
        return totals.getOrDefault(boxAt(pos), FluidStack.EMPTY).copy();
    }

    /** The one fluid this connected group holds or may hold, empty when every tank is empty. */
    public FluidStack groupFluid() {
        ensureTotals();
        for (FluidStack total : totals.values()) {
            if (!total.isEmpty()) {
                return total;
            }
        }
        return FluidStack.EMPTY;
    }

    public int fill(NodePos pos, FluidStack resource, FluidAction action) {
        ensureTotals();
        if (resource.isEmpty()) {
            return 0;
        }
        FluidStack group = groupFluid();
        if (!group.isEmpty() && !FluidStack.isSameFluidSameComponents(group, resource)) {
            return 0;
        }
        TankShape box = boxAt(pos);
        FluidStack total = totals.getOrDefault(box, FluidStack.EMPTY);
        int filled = Math.min(capacity(pos) - total.getAmount(), resource.getAmount());
        if (filled <= 0) {
            return 0;
        }
        if (action.execute()) {
            totals.put(box, total.isEmpty() ? resource.copyWithAmount(filled) : total.copyWithAmount(total.getAmount() + filled));
            layoutPending.add(box);
        }
        return filled;
    }

    public FluidStack drain(NodePos pos, int maxDrain, FluidAction action) {
        ensureTotals();
        TankShape box = boxAt(pos);
        FluidStack total = totals.getOrDefault(box, FluidStack.EMPTY);
        int drained = Math.min(maxDrain, total.getAmount());
        if (drained <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack result = total.copyWithAmount(drained);
        if (action.execute()) {
            int left = total.getAmount() - drained;
            totals.put(box, left == 0 ? FluidStack.EMPTY : total.copyWithAmount(left));
            layoutPending.add(box);
        }
        return result;
    }

    public FluidStack drain(NodePos pos, FluidStack resource, FluidAction action) {
        ensureTotals();
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(totals.getOrDefault(boxAt(pos), FluidStack.EMPTY), resource)) {
            return FluidStack.EMPTY;
        }
        return drain(pos, resource.getAmount(), action);
    }

    /** A block's share changed outside the network: re-sum on the next read. */
    void shareChangedOutside(FluidTankBlockEntity tank) {
        flush();
        totalsDirty = true;
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
        if (boxes.hasWaiting() && ++recheckCooldown >= RECHECK_INTERVAL) {
            recheckCooldown = 0;
            flush();
            if (boxes.recheck()) {
                topologyChanged();
            }
        }
        ensureTotals();
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

    /** Sets the six joined flags of every loaded member: joined toward blocks of the same tank. */
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
            TankShape box = boxAt(member);
            BlockState updated = state;
            for (Direction direction : Direction.values()) {
                NodePos neighbour = Conversions.toNodePos(pos.relative(direction));
                boolean joined = box.volume() > 1 && box.contains(neighbour) && nodes.containsKey(neighbour);
                updated = updated.setValue(FluidTankBlock.property(direction), joined);
            }
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }
}
