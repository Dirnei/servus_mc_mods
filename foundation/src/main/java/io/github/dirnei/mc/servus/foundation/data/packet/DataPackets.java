package io.github.dirnei.mc.servus.foundation.data.packet;

import io.github.dirnei.mc.servus.foundation.data.DataEvent;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNode;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Starts deliveries over the data line and finds them again by id. A send
 * hands over a stack the caller holds; a move takes items from the source
 * member as each packet leaves. Server thread only.
 */
public final class DataPackets {

    /** Ended deliveries kept for polling, oldest dropped first. */
    private static final int KEEP_ENDED = 1024;

    private static final AtomicLong NEXT_ID = new AtomicLong(1);
    private static final Map<Long, Delivery> DELIVERIES = new LinkedHashMap<>();

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
        if (like.isEmpty() || amount < 1) {
            return Optional.empty();
        }
        Delivery delivery = create(amount);
        start(level, from, to, delivery).ifPresent(network -> network.addShipment(Shipment.move(delivery, from, to, like, amount)));
        return Optional.of(delivery);
    }

    /** A delivery by id, while it runs and for a while after it ended. */
    public static synchronized Optional<Delivery> delivery(long id) {
        return Optional.ofNullable(DELIVERIES.get(id));
    }

    /** Settles every packet of every level; called before the server saves on stop. */
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

    private static synchronized Delivery create(int requested) {
        Delivery delivery = new Delivery(NEXT_ID.getAndIncrement(), requested);
        DELIVERIES.put(delivery.id(), delivery);
        if (DELIVERIES.size() > KEEP_ENDED) {
            for (Iterator<Delivery> it = DELIVERIES.values().iterator(); it.hasNext() && DELIVERIES.size() > KEEP_ENDED; ) {
                if (it.next().ended()) {
                    it.remove();
                }
            }
        }
        return delivery;
    }
}
