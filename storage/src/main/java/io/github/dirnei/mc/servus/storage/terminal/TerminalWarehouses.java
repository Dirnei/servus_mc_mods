package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The warehouses a block reaches over its data line: those whose controller
 * or one of whose Packing Stations is a member of its data network and that are
 * online. Server thread only.
 */
public final class TerminalWarehouses {

    /** The last merged stock per set of warehouses, with the index versions it was built from. */
    private record Merged(long[] versions, List<Map.Entry<ItemKey, Long>> stock) {
    }

    private static final int MERGED_KEEP = 32;

    /** Keys compare the warehouses by identity, since networks do not override equals. */
    private static final Map<List<WarehouseNetwork>, Merged> MERGED = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<List<WarehouseNetwork>, Merged> eldest) {
            return size() > MERGED_KEEP;
        }
    };

    private TerminalWarehouses() {
    }

    /** Reachable online warehouses, the one with the lowest controller position first. */
    public static List<WarehouseNetwork> reachable(ServerLevel level, BlockPos pos) {
        Optional<DataNetwork> data = DataNetworks.networkAt(level, pos);
        if (data.isEmpty()) {
            return List.of();
        }
        Set<WarehouseNetwork> found = Collections.newSetFromMap(new IdentityHashMap<>());
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
            network.filter(warehouse -> warehouse.status() == WarehouseNetwork.Status.ONLINE).ifPresent(found::add);
        }
        List<WarehouseNetwork> list = new ArrayList<>(found);
        list.sort(Comparator.comparingLong(warehouse -> warehouse.controllerPos().map(BlockPos::asLong).orElse(Long.MAX_VALUE)));
        return list;
    }

    /** The stock of all warehouses added up, largest first; the same list while no index changed. */
    public static List<Map.Entry<ItemKey, Long>> stock(List<WarehouseNetwork> warehouses) {
        long[] versions = new long[warehouses.size()];
        for (int i = 0; i < versions.length; i++) {
            versions[i] = warehouses.get(i).index().version();
        }
        List<WarehouseNetwork> key = List.copyOf(warehouses);
        Merged merged = MERGED.get(key);
        if (merged != null && Arrays.equals(merged.versions(), versions)) {
            return merged.stock();
        }
        if (warehouses.size() == 1) {
            List<Map.Entry<ItemKey, Long>> single = warehouses.get(0).index().entries();
            MERGED.put(key, new Merged(versions, single));
            return single;
        }
        Map<ItemKey, Long> totals = new LinkedHashMap<>();
        for (WarehouseNetwork warehouse : warehouses) {
            for (Map.Entry<ItemKey, Long> entry : warehouse.index().entries()) {
                totals.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
        }
        List<Map.Entry<ItemKey, Long>> list = new ArrayList<>(totals.size());
        totals.forEach((item, total) -> list.add(Map.entry(item, total)));
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        List<Map.Entry<ItemKey, Long>> stock = Collections.unmodifiableList(list);
        MERGED.put(key, new Merged(versions, stock));
        return stock;
    }
}
