package io.github.dirnei.mc.servus.storage.production;

import io.github.dirnei.mc.servus.foundation.adapter.MachineAdapterBlockEntity;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.pattern.PatternLibrary;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.OrderRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternsView;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.RecipeRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.StepRow;
import io.github.dirnei.mc.servus.storage.terminal.TerminalWarehouses;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Builds what a Production Terminal shows from the data line it touches. Server thread only. */
public final class ProductionView {

    /** Bounds the payload for producers that report very many recipes. */
    private static final int MAX_RECIPES = 64;

    private ProductionView() {
    }

    /** Whether a formed CPU or an online warehouse is on the line at {@code pos}. */
    public static boolean online(ServerLevel level, BlockPos pos) {
        Optional<DataNetwork> line = DataNetworks.networkAt(level, pos);
        return line.isPresent() && (!CpuOrders.cpus(level, line.get()).isEmpty()
                || !TerminalWarehouses.reachable(level, pos).isEmpty());
    }

    /** The current CPUs, orders and producers of the terminal's line. */
    public static ProductionPayload of(ServerLevel level, BlockPos pos) {
        Optional<DataNetwork> line = DataNetworks.networkAt(level, pos);
        if (line.isEmpty()) {
            return new ProductionPayload(pos, false, "no_line", List.of(), List.of(), PatternsView.EMPTY);
        }
        List<CpuRow> cpus = new ArrayList<>();
        for (CpuBlockEntity cpu : CpuOrders.cpus(level, line.get())) {
            cpus.add(cpuRow(level, cpu));
        }
        List<MachineRow> machines = machines(level, line.get());
        boolean online = !cpus.isEmpty() || !TerminalWarehouses.reachable(level, pos).isEmpty();
        return new ProductionPayload(pos, online, online ? "" : "nothing", cpus, machines, patterns(level, line.get()));
    }

    /** The patterns of the line with their storage and the line's capacity. */
    public static PatternsView patterns(ServerLevel level, DataNetwork line) {
        List<PatternRow> rows = new ArrayList<>();
        for (PatternLibrary.Entry entry : PatternLibrary.entries(level, line)) {
            rows.add(new PatternRow(entry.storage(), entry.slot(), entry.pattern().result(), entry.pattern().inputs()));
        }
        return new PatternsView(rows, rows.size(), PatternLibrary.capacity(level, line));
    }

    private static CpuRow cpuRow(ServerLevel level, CpuBlockEntity cpu) {
        int open = cpu.book().openCount();
        int slots = cpu.slots();
        List<OrderRow> orders = new ArrayList<>();
        for (WorkOrder order : cpu.book().orders()) {
            List<StepRow> steps = new ArrayList<>();
            for (OrderStep step : order.steps()) {
                steps.add(new StepRow(step.producer(), name(level, step.producer()), step.output(), step.batches(),
                        lower(step.state()), step.reason()));
            }
            orders.add(new OrderRow(order.id(), order.key().stack(1), order.amount(), lower(order.state()), order.reason(),
                    order.requestId(), steps));
        }
        return new CpuRow(cpu.getBlockPos(), slots, Math.min(open, slots), Math.max(0, open - slots), orders);
    }

    private static List<MachineRow> machines(ServerLevel level, DataNetwork line) {
        List<MachineRow> rows = new ArrayList<>();
        List<DataMember> producers = new ArrayList<>(line.dataMembers().stream()
                .filter(member -> member.has(DataRole.PRODUCER) || member.has(DataRole.UPKEEP)).toList());
        producers.sort(Comparator.comparingLong(member -> member.pos().asLong()));
        for (DataMember member : producers) {
            if (!level.isLoaded(member.pos())) {
                continue;
            }
            if (level.getBlockEntity(member.pos()) instanceof CraftingStationBlockEntity station) {
                // The CPU's hands: no recipe list, only the job it is crafting.
                ItemStack crafting = station.working() ? station.job().orElseThrow().result().copyWithCount(1) : ItemStack.EMPTY;
                rows.add(new MachineRow(member.pos(), name(level, member.pos()), crafting,
                        crafting.isEmpty() ? "station_idle" : "station_crafting", station.jobLeft(), List.of()));
                continue;
            }
            UpkeepHolder upkeep = level.getCapability(ServusFoundationCapabilities.UPKEEP, member.pos(), null);
            boolean keeps = upkeep != null && !upkeep.upkeepItem().isEmpty() && upkeep.upkeepLevel() > 0;
            ItemStack upkeepItem = keeps ? upkeep.upkeepItem().copyWithCount(1) : ItemStack.EMPTY;
            int upkeepCount = keeps ? upkeep.upkeepCount() : 0;
            int upkeepLevel = keeps ? upkeep.upkeepLevel() : 0;
            if (!member.has(DataRole.PRODUCER)) {
                rows.add(new MachineRow(member.pos(), name(level, member.pos()), ItemStack.EMPTY, "upkeep_only", 0, List.of(),
                        upkeepItem, upkeepCount, upkeepLevel));
                continue;
            }
            ProductionProvider provider = level.getCapability(ServusFoundationCapabilities.PRODUCTION, member.pos(), null);
            List<ProductionRecipe> recipes = provider == null ? List.of() : provider.recipes();
            List<RecipeRow> recipeRows = new ArrayList<>();
            for (ProductionRecipe recipe : recipes.subList(0, Math.min(recipes.size(), MAX_RECIPES))) {
                recipeRows.add(new RecipeRow(recipe.inputs(), recipe.outputs(), recipe.ticks()));
            }
            ItemStack working = ItemStack.EMPTY;
            Optional<ResourceLocation> active = provider == null ? Optional.empty() : provider.active();
            if (active.isPresent()) {
                for (ProductionRecipe recipe : recipes) {
                    if (recipe.id().equals(active.get()) && !recipe.outputs().isEmpty()) {
                        working = recipe.outputs().get(0).copyWithCount(1);
                        break;
                    }
                }
            }
            String state = !working.isEmpty() ? "working" : recipes.isEmpty() ? "no_recipes" : "idle";
            String machine = level.getBlockEntity(member.pos()) instanceof MachineAdapterBlockEntity adapter
                    ? adapter.machineName().getString() + " (adapter)" : name(level, member.pos());
            rows.add(new MachineRow(member.pos(), machine, working, state, 0, recipeRows, upkeepItem, upkeepCount, upkeepLevel));
        }
        return rows;
    }

    private static String name(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock().getName().getString();
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
