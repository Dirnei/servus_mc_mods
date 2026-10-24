package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.index.ReservationTally;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The warehouse a block's data line belongs to: the one whose controller or
 * one of whose Packing Stations is a member of that line, or a conflict when
 * the line reaches more than one. Server thread only.
 */
public final class TerminalWarehouses {

    /** The last requestable stock per warehouse, with the index and lock versions it was built from. */
    private record Cached(long index, long reservations, List<Map.Entry<ItemKey, Long>> stock) {
    }

    private static final int CACHED_KEEP = 32;

    /** Keys compare the warehouses by identity, since networks do not override equals. */
    private static final Map<WarehouseNetwork, Cached> CACHE = new IdentityHashMap<>();

    private TerminalWarehouses() {
    }

    /**
     * The warehouse of the data line at {@code pos}. Warehouses count whatever
     * their status, so an offline second warehouse still puts the line in
     * conflict; rail networks without a controller are no warehouse.
     */
    public static LineWarehouse line(ServerLevel level, BlockPos pos) {
        Optional<DataNetwork> data = DataNetworks.networkAt(level, pos);
        if (data.isEmpty()) {
            return LineWarehouse.NONE;
        }
        List<WarehouseNetwork> found = new ArrayList<>();
        for (DataMember member : data.get().dataMembers()) {
            if (!level.isLoaded(member.pos())) {
                continue;
            }
            BlockEntity entity = level.getBlockEntity(member.pos());
            Optional<WarehouseNetwork> network = Optional.empty();
            if (entity instanceof WarehouseControllerBlockEntity controller) {
                network = controller.network();
            } else if (entity instanceof PackingStationBlockEntity point) {
                network = point.network();
            }
            network.filter(warehouse -> warehouse.controllerPos().isPresent()).ifPresent(found::add);
        }
        return LineWarehouse.of(LineClassification.classify(found));
    }

    /** The online warehouse of the data line at {@code pos}; empty without one or on a line in conflict. */
    public static Optional<WarehouseNetwork> online(ServerLevel level, BlockPos pos) {
        return line(level, pos).online();
    }

    /**
     * What can still be requested from the warehouse: the indexed stock less
     * what picks have locked, items with nothing left dropped, largest first;
     * the same list while no index or lock changed.
     */
    public static List<Map.Entry<ItemKey, Long>> stock(WarehouseNetwork warehouse) {
        long index = warehouse.index().version();
        long reservations = warehouse.reservationVersion();
        Cached cached = CACHE.get(warehouse);
        if (cached != null && cached.index() == index && cached.reservations() == reservations) {
            return cached.stock();
        }
        List<Map.Entry<ItemKey, Long>> stock = ReservationTally.requestable(warehouse.index().entries(), warehouse.reservedOut());
        if (CACHE.size() >= CACHED_KEEP) {
            CACHE.clear();
        }
        CACHE.put(warehouse, new Cached(index, reservations, stock));
        return stock;
    }
}
