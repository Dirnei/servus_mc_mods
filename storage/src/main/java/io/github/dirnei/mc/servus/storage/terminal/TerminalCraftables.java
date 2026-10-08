package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** What the producers on a terminal's data line can make, for the stock grid and the craft dialog. Server thread only. */
public final class TerminalCraftables {

    private TerminalCraftables() {
    }

    /** The data line the terminal sits on. */
    public static Optional<DataNetwork> line(ServerLevel level, BlockPos terminal) {
        return DataNetworks.networkAt(level, terminal);
    }

    /** One item of every runnable recipe's first output on the terminal's line, sorted by name, at most {@code limit}. */
    public static List<ItemStack> of(ServerLevel level, BlockPos terminal, int limit) {
        Optional<DataNetwork> line = line(level, terminal);
        if (line.isEmpty()) {
            return List.of();
        }
        Map<ItemKey, ItemStack> found = new LinkedHashMap<>();
        for (OrderBook.Producer producer : CpuOrders.producers(level, line.get())) {
            for (ProductionRecipe recipe : producer.recipes()) {
                if (!recipe.outputs().isEmpty()) {
                    ItemStack output = recipe.outputs().get(0);
                    found.putIfAbsent(ItemKey.of(output), output.copyWithCount(1));
                }
            }
        }
        List<ItemStack> list = new ArrayList<>(found.values());
        list.sort(Comparator.comparing(stack -> stack.getHoverName().getString()));
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    /** What crafting the amount would take, craft and miss. */
    public static OrderBook.Preview preview(ServerLevel level, BlockPos terminal, ItemKey key, int amount) {
        return line(level, terminal).map(network -> CpuOrders.preview(level, network, key, amount))
                .orElse(OrderBook.Preview.refused("no_cpu"));
    }
}
