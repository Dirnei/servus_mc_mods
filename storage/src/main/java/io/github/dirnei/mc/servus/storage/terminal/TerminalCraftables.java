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
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** What the producers on a terminal's data line can make, for the stock grid and the craft dialog. Server thread only. */
public final class TerminalCraftables {

    /** The last craftable list per data line and the producer signature it was built from. */
    private record Cached(List<Long> key, List<ItemStack> items) {
    }

    /** Recipe lists longer than this are compared by identity; their providers cache them. */
    private static final int CONTENT_LIMIT = 64;

    private static final Map<DataNetwork, Cached> CACHE = new WeakHashMap<>();
    private static int builds;

    private TerminalCraftables() {
    }

    /** How many craftable lists were built from scratch, for tests. */
    public static int builds() {
        return builds;
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
        List<OrderBook.Producer> producers = CpuOrders.producers(level, line.get());
        List<Long> key = signature(producers);
        Cached cached = CACHE.get(line.get());
        if (cached == null || !cached.key().equals(key)) {
            cached = new Cached(key, build(producers));
            CACHE.put(line.get(), cached);
        }
        List<ItemStack> list = cached.items();
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    private static List<ItemStack> build(List<OrderBook.Producer> producers) {
        builds++;
        Map<ItemKey, ItemStack> found = new LinkedHashMap<>();
        for (OrderBook.Producer producer : producers) {
            for (ProductionRecipe recipe : producer.recipes()) {
                if (!recipe.outputs().isEmpty()) {
                    ItemStack output = recipe.outputs().get(0);
                    found.putIfAbsent(ItemKey.of(output), output.copyWithCount(1));
                }
            }
        }
        List<ItemStack> list = new ArrayList<>(found.values());
        list.sort(Comparator.comparing(stack -> stack.getHoverName().getString()));
        return List.copyOf(list);
    }

    /**
     * What the craftable list depends on: every producer's position and its
     * recipe list, long lists by identity (they come from a provider's cache
     * and change with a recipe reload), short ones by recipe id and output.
     */
    private static List<Long> signature(List<OrderBook.Producer> producers) {
        List<Long> key = new ArrayList<>(producers.size() * 2);
        for (OrderBook.Producer producer : producers) {
            key.add(producer.pos().asLong());
            List<ProductionRecipe> recipes = producer.recipes();
            long hash = recipes.size();
            if (recipes.size() > CONTENT_LIMIT) {
                hash = hash * 31 + System.identityHashCode(recipes);
            } else {
                for (ProductionRecipe recipe : recipes) {
                    hash = hash * 31 + recipe.id().hashCode();
                    if (!recipe.outputs().isEmpty()) {
                        ItemStack output = recipe.outputs().get(0);
                        hash = hash * 31 + output.getItem().hashCode();
                        hash = hash * 31 + output.getCount();
                    }
                }
            }
            key.add(hash);
        }
        return key;
    }

    /** What crafting the amount would take, craft and miss. */
    public static OrderBook.Preview preview(ServerLevel level, BlockPos terminal, ItemKey key, int amount) {
        return line(level, terminal).map(network -> CpuOrders.preview(level, network, key, amount))
                .orElse(OrderBook.Preview.refused("no_cpu"));
    }
}
