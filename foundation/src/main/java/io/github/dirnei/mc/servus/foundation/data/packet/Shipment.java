package io.github.dirnei.mc.servus.foundation.data.packet;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * One running delivery inside a data network: what is left to send or take,
 * the packet in flight and the items waiting at a full target. Server thread
 * only; ticked by its network.
 */
public final class Shipment {

    private final Delivery delivery;
    private final BlockPos from;
    private final BlockPos to;
    /** For a send: what has not left yet. */
    private ItemStack pending;
    /** For a move: what to take, and how many are still to take. */
    private final ItemStack like;
    private int toTake;

    private PacketFlight flight;
    private ItemStack cargo = ItemStack.EMPTY;
    private ItemStack waiting = ItemStack.EMPTY;
    private Direction targetFace = Direction.UP;
    private Direction sourceFace = Direction.UP;
    private int retry;
    private boolean broken;

    private Shipment(Delivery delivery, BlockPos from, BlockPos to, ItemStack pending, ItemStack like, int toTake) {
        this.delivery = delivery;
        this.from = from.immutable();
        this.to = to.immutable();
        this.pending = pending;
        this.like = like;
        this.toTake = toTake;
    }

    static Shipment send(Delivery delivery, BlockPos from, BlockPos to, ItemStack stack) {
        return new Shipment(delivery, from, to, stack.copy(), ItemStack.EMPTY, 0);
    }

    static Shipment move(Delivery delivery, BlockPos from, BlockPos to, ItemStack like, int amount) {
        return new Shipment(delivery, from, to, ItemStack.EMPTY, like.copyWithCount(1), amount);
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

    /** Marks the packet in flight when its route passes the removed node. */
    public void nodeRemoved(NodePos pos) {
        if (flight != null && flight.passes(pos)) {
            broken = true;
        }
    }

    /** Advances one tick. Returns true when the shipment is finished and can be dropped. */
    public boolean tick(DataNetwork network, ServerLevel level, PacketLimits limits) {
        if (!waiting.isEmpty()) {
            if (--retry <= 0) {
                retry = limits.retryTicks();
                deliver(level, waiting);
            }
        }
        if (flight != null) {
            if (broken) {
                reroute(network, level);
            } else {
                flight.advance(limits.blocksPerTick());
                if (flight.arrived()) {
                    targetFace = PacketIo.face(to, Conversions.toBlockPos(flight.lastNode()));
                    ItemStack arrived = cargo;
                    cargo = ItemStack.EMPTY;
                    flight = null;
                    retry = limits.retryTicks();
                    deliver(level, arrived);
                }
            }
        } else if (waiting.isEmpty() && !delivery.ended()) {
            launch(network, level, limits);
        }
        if (flight == null && waiting.isEmpty() && !delivery.ended() && nothingLeft()) {
            delivery.done();
        }
        return delivery.ended() && flight == null && waiting.isEmpty() && cargo.isEmpty();
    }

    /** Puts everything this shipment still holds somewhere safe and fails it if it was running. */
    public void settle(ServerLevel level, DeliveryReason why) {
        BlockPos last = flight != null ? Conversions.toBlockPos(flight.current()) : null;
        PacketIo.settle(level, cargo, to, targetFace, from, sourceFace, last);
        PacketIo.settle(level, waiting, to, targetFace, from, sourceFace, last);
        PacketIo.settle(level, pending, to, targetFace, from, sourceFace, last);
        cargo = ItemStack.EMPTY;
        waiting = ItemStack.EMPTY;
        pending = ItemStack.EMPTY;
        flight = null;
        delivery.fail(why);
    }

    private boolean nothingLeft() {
        return pending.isEmpty() && toTake <= 0;
    }

    private void deliver(ServerLevel level, ItemStack stack) {
        int before = stack.getCount();
        ItemStack rest = PacketIo.insert(level, to, targetFace, stack);
        delivery.addDelivered(before - rest.getCount());
        waiting = rest;
    }

    private void launch(DataNetwork network, ServerLevel level, PacketLimits limits) {
        if (nothingLeft()) {
            return;
        }
        Optional<List<NodePos>> route = network.packetRoute(from, to);
        if (route.isEmpty()) {
            settle(level, DeliveryReason.ROUTE_BROKEN);
            return;
        }
        List<NodePos> path = route.get();
        sourceFace = PacketIo.face(from, Conversions.toBlockPos(path.get(1)));
        targetFace = PacketIo.face(to, Conversions.toBlockPos(path.get(path.size() - 2)));
        ItemStack next;
        if (!pending.isEmpty()) {
            next = pending.split(limits.size());
        } else {
            next = PacketIo.extract(level, from, sourceFace, like, Math.min(limits.size(), toTake));
            if (next.isEmpty()) {
                toTake = 0;
                return;
            }
            toTake -= next.getCount();
        }
        cargo = next;
        flight = new PacketFlight(path);
        broken = false;
    }

    private void reroute(DataNetwork network, ServerLevel level) {
        NodePos at = flight.current();
        Optional<List<NodePos>> route = at.equals(flight.source())
                ? network.packetRoute(from, to)
                : network.packetRouteFrom(at, to);
        if (route.isPresent()) {
            flight = new PacketFlight(route.get());
            broken = false;
        } else {
            settle(level, DeliveryReason.ROUTE_BROKEN);
        }
    }
}
