package io.github.dirnei.mc.servus.foundation.data.packet;

import io.github.dirnei.mc.servus.foundation.data.DataEvent;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNode;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;

/**
 * Starts deliveries over the data line. The data
 * line is instant: items do not travel, each tick a delivery moves what the
 * source gives, the target takes and the transfer gates of both allow. A send
 * hands over a stack the caller holds; a move takes items from the source
 * member only as far as the target accepts them, so items wait at their
 * source, never in flight. A target with a {@link PacketEndpoint} is offered
 * the items first and the delivery sends only what it grants. Server thread only.
 */
public final class DataPackets {

    private static final AtomicLong NEXT_ID = new AtomicLong(1);
    private static final Set<Long> RUNNING = new HashSet<>();

    private DataPackets() {
    }

    /**
     * Sends a stack the caller holds from {@code from} to the member at
     * {@code to}. When the delivery fails at once (no route, no target, or an
     * endpoint that grants less than the whole stack), the caller keeps its
     * stack; otherwise the network owns it.
     */
    public static Optional<Delivery> send(ServerLevel level, BlockPos from, BlockPos to, ItemStack stack) {
        return send(level, from, to, stack, null);
    }

    /** Like {@link #send(ServerLevel, BlockPos, BlockPos, ItemStack)} into a grant the caller already holds. */
    public static Optional<Delivery> send(ServerLevel level, BlockPos from, BlockPos to, ItemStack stack, Grant held) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Start start = start(level, from, to, stack, stack.getCount(), held, true);
        if (start.failed() != null) {
            return Optional.of(start.failed());
        }
        Delivery delivery = create(stack.getCount());
        bind(delivery, start.grant());
        start.network().addShipment(Shipment.send(delivery, from, to, stack, start.grant()));
        return Optional.of(delivery);
    }

    /** Moves up to {@code amount} items like {@code like} from the member at {@code from} to the member at {@code to}. */
    public static Optional<Delivery> move(ServerLevel level, BlockPos from, BlockPos to, ItemStack like, int amount) {
        return move(level, from, to, like, amount, null);
    }

    /**
     * Like {@link #move(ServerLevel, BlockPos, BlockPos, ItemStack, int)}, but
     * takes the items from {@code source} instead of the source member's
     * capabilities, for a member whose public handler does not give items out
     * (a terminal's input row, a Packing Station's outgoing buffer). The
     * member at {@code from} still decides the route and its transfer gate.
     */
    public static Optional<Delivery> move(ServerLevel level, BlockPos from, BlockPos to, ItemStack like, int amount,
                                          ItemView source) {
        return move(level, from, to, like, amount, source, null);
    }

    /**
     * Like {@link #move(ServerLevel, BlockPos, BlockPos, ItemStack, int, ItemView)}
     * into a grant the caller already holds instead of offering again. The
     * delivery asks for no more than the grant still takes and closes the
     * grant when it ends; a delivery that cannot start closes it at once.
     */
    public static Optional<Delivery> move(ServerLevel level, BlockPos from, BlockPos to, ItemStack like, int amount,
                                          ItemView source, Grant held) {
        if (like.isEmpty() || amount < 1) {
            if (held != null) {
                held.close();
            }
            return Optional.empty();
        }
        Start start = start(level, from, to, like, amount, held, false);
        if (start.failed() != null) {
            return Optional.of(start.failed());
        }
        int count = start.grant() == null ? amount : Math.min(amount, start.grant().left());
        Delivery delivery = create(count);
        bind(delivery, start.grant());
        start.network().addShipment(Shipment.move(delivery, from, to, like, count, source, start.grant()));
        return Optional.of(delivery);
    }

    /** Whether the delivery with that id is still running. */
    public static boolean running(long deliveryId) {
        return RUNNING.contains(deliveryId);
    }

    /** Settles every running delivery of every level; called before the server saves on stop. */
    public static void settleAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            NetworkManager.of(level).set(DataNetwork.TYPE).networks().forEach(DataNetwork::settleAll);
        }
    }

    /**
     * How a delivery starts: the network and grant it runs with, or the
     * delivery that already failed (no route, a target that takes nothing, a
     * refusal, or for a whole stack a grant smaller than the stack).
     */
    private record Start(DataNetwork network, Grant grant, Delivery failed) {
    }

    private static Start start(ServerLevel level, BlockPos from, BlockPos to, ItemStack like, int amount, Grant held, boolean whole) {
        Optional<Route> route = route(level, from, to);
        if (route.isEmpty() || route.get().failure() != null) {
            return failed(amount, held, delivery -> delivery.fail(route.map(Route::failure).orElse(DeliveryReason.NO_ROUTE)));
        }
        Grant grant = held;
        if (grant == null) {
            PacketEndpoint endpoint = PacketIo.endpoint(level, to, route.get().face());
            grant = endpoint == null ? null : endpoint.offer(like.copyWithCount(1), amount);
        }
        if (grant != null && (!grant.granted() || grant.left() < 1 || whole && grant.left() < amount)) {
            String why = grant.refusal();
            return failed(amount, grant, delivery -> delivery.refuse(why));
        }
        return new Start(route.get().network(), grant, null);
    }

    private static Start failed(int amount, Grant grant, Consumer<Delivery> fail) {
        Delivery delivery = create(amount);
        fail.accept(delivery);
        if (grant != null) {
            grant.close();
        }
        return new Start(null, null, delivery);
    }

    private static void bind(Delivery delivery, Grant grant) {
        if (grant != null) {
            grant.bind(delivery.id());
            delivery.onEnd(ended -> grant.close());
        }
    }

    /** The network linking both positions and the target's face on the line, with a failure when the target takes nothing; empty without a route. */
    private record Route(DataNetwork network, Direction face, DeliveryReason failure) {
    }

    private static Optional<Route> route(ServerLevel level, BlockPos from, BlockPos to) {
        NetworkSet<DataNode, DataEvent, DataNetwork> set = NetworkManager.of(level).set(DataNetwork.TYPE);
        for (DataNetwork network : set.networks()) {
            if (network.reaches(from) && network.reaches(to)) {
                Optional<List<NodePos>> route = network.packetRoute(from, to);
                if (route.isEmpty()) {
                    return Optional.empty();
                }
                List<NodePos> path = route.get();
                Direction face = PacketIo.face(to, Conversions.toBlockPos(path.get(path.size() - 2)));
                return Optional.of(new Route(network, face, PacketIo.accepts(level, to, face) ? null : DeliveryReason.NO_TARGET));
            }
        }
        return Optional.empty();
    }

    private static Delivery create(int requested) {
        Delivery delivery = new Delivery(NEXT_ID.getAndIncrement(), requested);
        RUNNING.add(delivery.id());
        delivery.onEnd(ended -> RUNNING.remove(ended.id()));
        return delivery;
    }
}
