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
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;

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
    private final ItemView sourceView;
    /** The room the target's endpoint reserved for this shipment, null for a target without endpoint. */
    private final Grant grant;

    private Direction targetFace = Direction.UP;
    private Direction sourceFace = Direction.UP;
    private boolean routed;
    private List<DataNetwork.Hop> hops = List.of();
    private int retry;
    /** Game time since which a full target took nothing, -1 while items move. */
    private long blockedSince = -1;

    private Shipment(Delivery delivery, BlockPos from, BlockPos to, ItemStack pending, ItemStack like, int toTake,
                     ItemView sourceView, Grant grant) {
        this.delivery = delivery;
        this.from = from.immutable();
        this.to = to.immutable();
        this.pending = pending;
        this.like = like;
        this.toTake = toTake;
        this.sourceView = sourceView;
        this.grant = grant;
    }

    static Shipment send(Delivery delivery, BlockPos from, BlockPos to, ItemStack stack, Grant grant) {
        return new Shipment(delivery, from, to, stack.copy(), ItemStack.EMPTY, 0, null, grant);
    }

    static Shipment move(Delivery delivery, BlockPos from, BlockPos to, ItemStack like, int amount, ItemView sourceView, Grant grant) {
        return new Shipment(delivery, from, to, ItemStack.EMPTY, like.copyWithCount(1), amount, sourceView, grant);
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
        if (stalled(level, limits)) {
            settle(level, DeliveryReason.STALLED);
            return pending.isEmpty();
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
            blockedSince = -1;
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

    /** Whether a delivery without a grant found its target full for {@code stall_ticks}. */
    private boolean stalled(ServerLevel level, PacketLimits limits) {
        return grant == null && blockedSince >= 0 && level.getGameTime() - blockedSince >= limits.stallTicks();
    }

    /**
     * The target took less than offered: waits {@code retry_interval} and counts towards a stall. A loaded target that
     * takes no items at all any more, because its block was broken or replaced, fails the shipment at once.
     */
    private void waitForTarget(ServerLevel level, PacketLimits limits) {
        if (grant == null && level.isLoaded(to) && !PacketIo.accepts(level, to, targetFace)) {
            settle(level, DeliveryReason.NO_TARGET);
            return;
        }
        retry = limits.retryTicks();
        if (blockedSince < 0) {
            blockedSince = level.getGameTime();
        }
    }

    private static int allowance(TransferGate gate, TransferGate.Flow flow) {
        return gate == null ? TransferAmount.NO_GATE : Math.max(0, gate.allowance(flow));
    }

    /** Offers part of the held stack to the target; what does not fit is offered again after the retry interval. */
    private int offer(ServerLevel level, int allowed, PacketLimits limits) {
        if (grantUsedUp()) {
            settle(level, DeliveryReason.NO_TARGET);
            return 0;
        }
        int count = Math.min(allowed, pending.getCount());
        ItemStack part = pending.split(count);
        ItemStack rest = insert(level, part);
        if (!rest.isEmpty()) {
            if (pending.isEmpty()) {
                pending = rest;
            } else {
                pending.grow(rest.getCount());
            }
            waitForTarget(level, limits);
        }
        return count - rest.getCount();
    }

    /** Whether the grant takes nothing more, because it is full or its endpoint closed it. */
    private boolean grantUsedUp() {
        return grant != null && grant.left() <= 0;
    }

    /** Puts items into the target: into the grant's room when there is one, else through its inlet or sink. */
    private ItemStack insert(ServerLevel level, ItemStack stack) {
        return grant != null ? grant.deliver(stack, level.getGameTime()) : PacketIo.insert(level, to, targetFace, stack);
    }

    /** Takes only what the target accepts from the source and inserts it; a full target is asked again later. */
    private int take(ServerLevel level, int allowed, PacketLimits limits) {
        if (grantUsedUp()) {
            toTake = 0;
            return 0;
        }
        int max = Math.min(allowed, toTake);
        int has = PacketIo.available(level, from, sourceFace, sourceView, like, max);
        if (has <= 0) {
            toTake = 0;
            return 0;
        }
        int takes = grant != null ? Math.min(has, grant.left()) : PacketIo.acceptable(level, to, targetFace, like.copyWithCount(has));
        if (takes <= 0) {
            waitForTarget(level, limits);
            return 0;
        }
        ItemStack got = PacketIo.extract(level, from, sourceFace, sourceView, like, takes);
        if (got.isEmpty()) {
            toTake = 0;
            return 0;
        }
        toTake -= got.getCount();
        ItemStack rest = insert(level, got);
        int moved = got.getCount() - rest.getCount();
        if (!rest.isEmpty()) {
            toTake += rest.getCount();
            pending = PacketIo.giveBack(level, from, sourceFace, sourceView, rest);
            if (!pending.isEmpty()) {
                toTake -= pending.getCount();
            }
            waitForTarget(level, limits);
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
