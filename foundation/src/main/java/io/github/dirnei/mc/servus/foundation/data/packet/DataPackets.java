package io.github.dirnei.mc.servus.foundation.data.packet;

import io.github.dirnei.mc.servus.foundation.data.DataEvent;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNode;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
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
 * source, never in flight. Server thread only.
 */
public final class DataPackets {

    private static final AtomicLong NEXT_ID = new AtomicLong(1);

    private DataPackets() {
    }

    /**
     * Sends a stack the caller holds from {@code from} to the member at
     * {@code to}. When the delivery fails at once (no route, no target), the
     * caller keeps its stack; otherwise the network owns it.
     */
    public static Optional<Delivery> send(ServerLevel level, BlockPos from, BlockPos to, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Delivery delivery = create(stack.getCount());
        start(level, from, to, delivery).ifPresent(network -> network.addShipment(Shipment.send(delivery, from, to, stack)));
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
        if (like.isEmpty() || amount < 1) {
            return Optional.empty();
        }
        Delivery delivery = create(amount);
        start(level, from, to, delivery)
                .ifPresent(network -> network.addShipment(Shipment.move(delivery, from, to, like, amount, source)));
        return Optional.of(delivery);
    }

    /** Settles every running delivery of every level; called before the server saves on stop. */
    public static void settleAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            NetworkManager.of(level).set(DataNetwork.TYPE).networks().forEach(DataNetwork::settleAll);
        }
    }

    /** The network that links both positions with a route, failing the delivery when there is none. */
    private static Optional<DataNetwork> start(ServerLevel level, BlockPos from, BlockPos to, Delivery delivery) {
        NetworkSet<DataNode, DataEvent, DataNetwork> set = NetworkManager.of(level).set(DataNetwork.TYPE);
        for (DataNetwork network : set.networks()) {
            if (network.reaches(from) && network.reaches(to)) {
                Optional<List<NodePos>> route = network.packetRoute(from, to);
                if (route.isEmpty()) {
                    break;
                }
                List<NodePos> path = route.get();
                BlockPos last = Conversions.toBlockPos(path.get(path.size() - 2));
                if (!PacketIo.accepts(level, to, PacketIo.face(to, last))) {
                    delivery.fail(DeliveryReason.NO_TARGET);
                    return Optional.empty();
                }
                return Optional.of(network);
            }
        }
        delivery.fail(DeliveryReason.NO_ROUTE);
        return Optional.empty();
    }

    private static Delivery create(int requested) {
        return new Delivery(NEXT_ID.getAndIncrement(), requested);
    }
}
