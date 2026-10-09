package io.github.dirnei.mc.servus.foundation.data.packet;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.production.TransferGate;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * One running delivery inside a data network. The network is instant: each
 * tick the shipment moves as many items as the source can give, the target
 * takes and the transfer gates of source and target allow. A move takes items
 * from its source only as far as the target accepts them, so nothing is in
 * flight; a send holds the stack its caller handed over until it is placed.
 * Server thread only; ticked by its network.
 */
public final class Shipment {

    private final Delivery delivery;
    private final BlockPos from;
    private final BlockPos to;
    /** Items this shipment holds: a handed over stack, or what a move could not give back. */
    private ItemStack pending;
    /** For a move: what to take, how many are still to take, and an optional view of the source. */
    private final ItemStack like;
    private int toTake;
    private final IItemHandler sourceView;

    private Direction targetFace = Direction.UP;
    private Direction sourceFace = Direction.UP;
    private boolean routed;
    private List<DataNetwork.Hop> hops = List.of();
    private int retry;

    private Shipment(Delivery delivery, BlockPos from, BlockPos to, ItemStack pending, ItemStack like, int toTake,
                     IItemHandler sourceView) {
        this.delivery = delivery;
        this.from = from.immutable();
        this.to = to.immutable();
        this.pending = pending;
        this.like = like;
        this.toTake = toTake;
        this.sourceView = sourceView;
    }

    static Shipment send(Delivery delivery, BlockPos from, BlockPos to, ItemStack stack) {
        return new Shipment(delivery, from, to, stack.copy(), ItemStack.EMPTY, 0, null);
    }

    static Shipment move(Delivery delivery, BlockPos from, BlockPos to, ItemStack like, int amount, IItemHandler sourceView) {
        return new Shipment(delivery, from, to, ItemStack.EMPTY, like.copyWithCount(1), amount, sourceView);
    }

    public Delivery delivery() {
        return delivery;
    }

    public BlockPos from() {
        return from;
    }

    public BlockPos to() {
        return to;
    }

    /** The graph or a tier changed: the route is picked again before the next items move. */
    public void reroute() {
        routed = false;
    }

    /** The ducts this shipment crosses, empty before it is routed. */
    public List<DataNetwork.Hop> hops() {
        return hops;
    }

    /** Advances one tick. Returns true when the shipment is finished and can be dropped. */
    public boolean tick(DataNetwork network, ServerLevel level, PacketLimits limits) {
        if (delivery.ended()) {
            return pending.isEmpty();
        }
        if (!routed && !route(network, level)) {
            return true;
        }
        if (retry > 0 && --retry > 0) {
            return false;
        }
        TransferGate out = PacketIo.gate(level, from, sourceFace);
        TransferGate in = PacketIo.gate(level, to, targetFace);
        int left = pending.isEmpty() ? toTake : pending.getCount();
        int allowed = TransferAmount.gates(left, allowance(out, TransferGate.Flow.OUT), allowance(in, TransferGate.Flow.IN));
        // Every duct on the path limits it as well; deliveries that cross a duct share its budget in creation order.
        allowed = Math.min(allowed, network.pathAllowance(hops));
        int moved = 0;
        if (allowed > 0) {
            moved = pending.isEmpty() ? take(level, allowed, limits) : offer(level, allowed, limits);
        }
        if (moved > 0) {
            delivery.addDelivered(moved);
            if (out != null) {
                out.consume(TransferGate.Flow.OUT, moved);
            }
            if (in != null) {
                in.consume(TransferGate.Flow.IN, moved);
            }
            network.consumePath(hops, moved);
        }
        if (pending.isEmpty() && toTake <= 0) {
            delivery.done();
        }
        return delivery.ended() && pending.isEmpty();
    }

    private static int allowance(TransferGate gate, TransferGate.Flow flow) {
        return gate == null ? TransferAmount.NO_GATE : Math.max(0, gate.allowance(flow));
    }

    /** Offers part of the held stack to the target; what does not fit is offered again after the retry interval. */
    private int offer(ServerLevel level, int allowed, PacketLimits limits) {
        int count = Math.min(allowed, pending.getCount());
        ItemStack part = pending.split(count);
        ItemStack rest = PacketIo.insert(level, to, targetFace, part);
        if (!rest.isEmpty()) {
            if (pending.isEmpty()) {
                pending = rest;
            } else {
                pending.grow(rest.getCount());
            }
            retry = limits.retryTicks();
        }
        return count - rest.getCount();
    }

    /** Takes only what the target accepts from the source and inserts it; a full target is asked again later. */
    private int take(ServerLevel level, int allowed, PacketLimits limits) {
        int max = Math.min(allowed, toTake);
        int has = PacketIo.available(level, from, sourceFace, sourceView, like, max);
        if (has <= 0) {
            // The source ran out: the move ends with what it moved.
            toTake = 0;
            return 0;
        }
        int takes = PacketIo.acceptable(level, to, targetFace, like.copyWithCount(has));
        if (takes <= 0) {
            retry = limits.retryTicks();
            return 0;
        }
        ItemStack got = PacketIo.extract(level, from, sourceFace, sourceView, like, takes);
        if (got.isEmpty()) {
            toTake = 0;
            return 0;
        }
        toTake -= got.getCount();
        ItemStack rest = PacketIo.insert(level, to, targetFace, got);
        int moved = got.getCount() - rest.getCount();
        if (!rest.isEmpty()) {
            toTake += rest.getCount();
            pending = PacketIo.giveBack(level, from, sourceFace, sourceView, rest);
            if (!pending.isEmpty()) {
                toTake -= pending.getCount();
            }
            retry = limits.retryTicks();
        }
        return moved;
    }

    /** Finds the faces that touch the line; settles and fails the shipment when no route is left. */
    private boolean route(DataNetwork network, ServerLevel level) {
        Optional<List<NodePos>> route = network.packetRoute(from, to);
        if (route.isEmpty()) {
            settle(level, DeliveryReason.ROUTE_BROKEN);
            return false;
        }
        List<NodePos> path = route.get();
        sourceFace = PacketIo.face(from, Conversions.toBlockPos(path.get(1)));
        targetFace = PacketIo.face(to, Conversions.toBlockPos(path.get(path.size() - 2)));
        hops = network.hops(path);
        routed = true;
        return true;
    }

    /**
     * Puts everything this shipment still holds somewhere safe and fails it if
     * it was running. A move leaves what it has not moved at its source.
     */
    public void settle(ServerLevel level, DeliveryReason why) {
        if (!pending.isEmpty()) {
            ItemStack rest = sourceView != null ? PacketIo.giveBack(level, from, sourceFace, sourceView, pending) : pending;
            PacketIo.settle(level, rest, to, targetFace, from, sourceFace, null);
        }
        pending = ItemStack.EMPTY;
        toTake = 0;
        delivery.fail(why);
    }
}
