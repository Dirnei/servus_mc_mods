package io.github.dirnei.mc.servus.storage.cpu;

import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.Collections;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Hands work orders to the Crafting CPUs of a data line and answers questions
 * about them. Server thread only.
 */
public final class CpuOrders {

    /** An order placed on a CPU, or why none was made ({@code no_cpu}, {@code no_producer}, {@code in_stock}). */
    public record Placed(OrderBook.Created created, BlockPos cpu) {
        public boolean made() {
            return created.made();
        }

        public String refusal() {
            return created.refusal();
        }
    }

    private CpuOrders() {
    }

    /**
     * The data line found from a position: a data member or cable, a CPU
     * block, or a warehouse block whose Access Points are on a line.
     */
    public static Optional<DataNetwork> network(ServerLevel level, BlockPos pos) {
        Optional<DataNetwork> direct = DataNetworks.networkAt(level, pos);
        if (direct.isPresent()) {
            return direct;
        }
        CpuBlockEntity cpu = CpuBlock.master(level, pos);
        if (cpu != null) {
            return cpu.dataNetwork();
        }
        if (level.getBlockEntity(pos) instanceof StorageNodeBlockEntity node) {
            return node.network().flatMap(warehouse -> network(level, warehouse));
        }
        return Optional.empty();
    }

    /** The data line of a warehouse: the controller's own, else that of its first Access Point on a line. */
    public static Optional<DataNetwork> network(ServerLevel level, WarehouseNetwork warehouse) {
        Optional<DataNetwork> fromController = warehouse.controllerPos().flatMap(pos -> DataNetworks.networkAt(level, pos));
        if (fromController.isPresent()) {
            return fromController;
        }
        for (BlockPos point : warehouse.accessPoints()) {
            Optional<DataNetwork> line = DataNetworks.networkAt(level, point);
            if (line.isPresent()) {
                return line;
            }
        }
        return Optional.empty();
    }

    /** The formed CPUs on the line, lowest position first. */
    public static List<CpuBlockEntity> cpus(ServerLevel level, DataNetwork network) {
        Set<CpuBlockEntity> found = Collections.newSetFromMap(new IdentityHashMap<>());
        for (DataMember member : network.dataMembers(DataRole.LINK)) {
            if (level.isLoaded(member.pos())) {
                CpuBlockEntity cpu = CpuBlock.master(level, member.pos());
                if (cpu != null) {
                    found.add(cpu);
                }
            }
        }
        List<CpuBlockEntity> list = new ArrayList<>(found);
        list.sort(Comparator.comparingLong(cpu -> cpu.getBlockPos().asLong()));
        return list;
    }

    public static List<CpuBlockEntity> cpus(ServerLevel level, BlockPos pos) {
        return network(level, pos).map(network -> cpus(level, network)).orElse(List.of());
    }

    /** Makes an order on the CPU of the line that {@link CpuChoice} picks. */
    public static Placed create(ServerLevel level, DataNetwork network, ItemKey key, int amount, int requestId) {
        List<CpuBlockEntity> cpus = cpus(level, network);
        List<CpuChoice.Load> loads = new ArrayList<>();
        for (CpuBlockEntity cpu : cpus) {
            loads.add(new CpuChoice.Load(cpu.book().openCount(), cpu.slots()));
        }
        OptionalInt chosen = CpuChoice.choose(loads);
        if (chosen.isEmpty()) {
            return new Placed(new OrderBook.Created(null, "no_cpu"), null);
        }
        CpuBlockEntity cpu = cpus.get(chosen.getAsInt());
        return new Placed(cpu.book().create(key, amount, requestId), cpu.getBlockPos());
    }

    public static Placed create(ServerLevel level, BlockPos pos, ItemKey key, int amount, int requestId) {
        Optional<DataNetwork> network = network(level, pos);
        if (network.isEmpty()) {
            return new Placed(new OrderBook.Created(null, "no_cpu"), null);
        }
        return create(level, network.get(), key, amount, requestId);
    }

    /** The CPU whose master sits at the position, if loaded and formed. */
    public static Optional<CpuBlockEntity> cpuAt(ServerLevel level, BlockPos master) {
        if (master == null || !level.isLoaded(master)) {
            return Optional.empty();
        }
        return Optional.ofNullable(CpuBlock.master(level, master));
    }

    /** Amount of the key on open orders of every CPU of the line. */
    public static int openAmountFor(ServerLevel level, DataNetwork network, ItemKey key) {
        int sum = 0;
        for (CpuBlockEntity cpu : cpus(level, network)) {
            sum += cpu.book().openAmountFor(key);
        }
        return sum;
    }

    /** Stock reserved by the CPUs of the line other than the one owning {@code except}. */
    public static Map<ItemKey, Integer> reservedOnLine(ServerLevel level, DataNetwork network, OrderBook except) {
        Map<ItemKey, Integer> total = new HashMap<>();
        for (CpuBlockEntity cpu : cpus(level, network)) {
            if (cpu.book() != except) {
                cpu.book().reservedTotal().forEach((key, count) -> total.merge(key, count, Integer::sum));
            }
        }
        return total;
    }

    /** Whether a CPU of the line other than the one owning {@code except} has a step on the producer. */
    public static boolean busyElsewhere(ServerLevel level, DataNetwork network, BlockPos producer, OrderBook except) {
        for (CpuBlockEntity cpu : cpus(level, network)) {
            if (cpu.book() != except && cpu.book().occupies(producer)) {
                return true;
            }
        }
        return false;
    }

    /** Cancels the orders of a request on every CPU of the line found from the position. */
    public static void cancelForRequest(ServerLevel level, BlockPos pos, int requestId) {
        for (CpuBlockEntity cpu : cpus(level, pos)) {
            cpu.book().cancelForRequest(requestId);
        }
    }
}
