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
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternsView;
import io.github.dirnei.mc.servus.storage.terminal.TerminalWarehouses;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.JobRow;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
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

    private ProductionView() {
    }

    /** Whether a formed CPU or an online warehouse is on the line at {@code pos}. */
    public static boolean online(ServerLevel level, BlockPos pos) {
        Optional<DataNetwork> line = DataNetworks.networkAt(level, pos);
        return line.isPresent() && (!CpuOrders.cpus(level, line.get()).isEmpty()
                || !TerminalWarehouses.reachable(level, pos).isEmpty());
    }

    /** The current CPUs, jobs, machines and patterns of the terminal's line. */
    public static ProductionPayload of(ServerLevel level, BlockPos pos) {
        Optional<DataNetwork> line = DataNetworks.networkAt(level, pos);
        if (line.isEmpty()) {
            return new ProductionPayload(pos, false, "no_line", List.of(), List.of(), List.of(), PatternsView.EMPTY);
        }
        List<CpuRow> cpus = new ArrayList<>();
        List<JobRow> jobs = new ArrayList<>();
        Map<BlockPos, String> blocked = new HashMap<>();
        for (CpuBlockEntity cpu : CpuOrders.cpus(level, line.get())) {
            int open = cpu.book().openCount();
            int slots = cpu.slots();
            cpus.add(new CpuRow(cpu.getBlockPos(), slots, Math.min(open, slots), Math.max(0, open - slots)));
            for (WorkOrder order : cpu.book().orders()) {
                jobs.add(job(level, cpu.getBlockPos(), order));
                if (order.open()) {
                    for (OrderStep step : order.steps()) {
                        boolean waits = step.state() == OrderStep.State.WAITING_STATION || step.state() == OrderStep.State.DELIVERING;
                        if (waits && !step.reason().isEmpty()) {
                            blocked.putIfAbsent(step.producer(), step.reason());
                        }
                    }
                }
            }
        }
        // Open jobs first, then oldest first; ids grow per CPU, so CPU then id keeps creation order per CPU.
        jobs.sort(Comparator.comparing((JobRow job) -> job.open() ? 0 : 1)
                .thenComparingLong(job -> job.cpu().asLong()).thenComparingInt(JobRow::id));
        List<MachineRow> machines = machines(level, line.get(), blocked);
        boolean online = !cpus.isEmpty() || !TerminalWarehouses.reachable(level, pos).isEmpty();
        return new ProductionPayload(pos, online, online ? "" : "nothing", cpus, jobs, machines, patterns(level, line.get()));
    }

    /** The patterns of the line with their storage and the line's capacity. */
    public static PatternsView patterns(ServerLevel level, DataNetwork line) {
        List<PatternRow> rows = new ArrayList<>();
        for (PatternLibrary.Entry entry : PatternLibrary.entries(level, line)) {
            rows.add(new PatternRow(entry.storage(), entry.slot(), entry.pattern().result(), entry.pattern().inputs()));
        }
        return new PatternsView(rows, rows.size(), PatternLibrary.capacity(level, line));
    }

    private static JobRow job(ServerLevel level, BlockPos cpu, WorkOrder order) {
        ItemKey key = order.key();
        int crafted = 0;
        OrderStep current = null;
        for (OrderStep step : order.steps()) {
            if (step.produces(key)) {
                crafted += step.collected(key);
            }
            if (current == null && step.state() != OrderStep.State.DONE) {
                current = step;
            }
        }
        String state = lower(order.state());
        crafted = state.equals("done") ? order.amount() : Math.min(order.amount(), crafted);
        String missingName = "";
        int missingCount = 0;
        for (Map.Entry<ItemKey, Integer> missing : order.missing().entrySet()) {
            if (missing.getValue() > 0) {
                missingName = missing.getKey().stack(1).getHoverName().getString();
                missingCount = missing.getValue();
                break;
            }
        }
        JobStatus.Step step = null;
        if (current != null) {
            ItemStack output = current.output();
            int perCraft = output.isEmpty() ? 0 : output.getCount();
            int done = output.isEmpty() ? 0 : JobStatus.craftsDone(current.collected(ItemKey.of(output)), perCraft, current.batches());
            step = new JobStatus.Step(machineName(level, current.producer()), lower(current.state()), current.reason(),
                    verb(level, current.recipe().id()), done, current.batches(), outOfFuel(level, current.producer()));
        }
        JobStatus.Status status = JobStatus.of(state, order.reason(), missingName, missingCount, step);
        return new JobRow(cpu, order.id(), key.stack(1), order.amount(), crafted, state, order.open() ? -1 : order.doneAt(),
                status.key(), status.args(), order.requestId());
    }

    /** crafting, smelting, ... from the recipe's type, working when the recipe is not a known one. */
    private static String verb(ServerLevel level, ResourceLocation recipe) {
        return level.getRecipeManager().byKey(recipe)
                .map(holder -> BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType()))
                .map(ResourceLocation::getPath)
                .orElse("working");
    }

    private static boolean outOfFuel(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        UpkeepHolder upkeep = level.getCapability(ServusFoundationCapabilities.UPKEEP, pos, null);
        return upkeep != null && !upkeep.upkeepItem().isEmpty() && upkeep.upkeepLevel() > 0 && upkeep.upkeepCount() <= 0;
    }

    private static List<MachineRow> machines(ServerLevel level, DataNetwork line, Map<BlockPos, String> blocked) {
        List<MachineRow> rows = new ArrayList<>();
        for (DataMember member : line.dataMembers()) {
            if (!(member.has(DataRole.PRODUCER) || member.has(DataRole.UPKEEP)) || !level.isLoaded(member.pos())) {
                continue;
            }
            rows.add(machine(level, member, blocked.getOrDefault(member.pos(), "")));
        }
        rows.sort(MachineOrder.comparator(MachineRow::state, row -> row.pos().asLong()));
        return rows;
    }

    private static MachineRow machine(ServerLevel level, DataMember member, String blockedReason) {
        BlockPos pos = member.pos();
        ItemStack icon = new ItemStack(level.getBlockState(pos).getBlock());
        if (level.getBlockEntity(pos) instanceof CraftingStationBlockEntity station) {
            // The CPU's hands: no recipe list, only the job it is crafting.
            if (station.working()) {
                return new MachineRow(pos, icon, name(level, pos), "crafting", station.job().orElseThrow().result().copyWithCount(1),
                        station.jobLeft(), "", ItemStack.EMPTY, 0, 0, -1);
            }
            String state = blockedReason.isEmpty() ? "idle" : "blocked";
            return new MachineRow(pos, icon, name(level, pos), state, ItemStack.EMPTY, 0, blockedReason, ItemStack.EMPTY, 0, 0, -1);
        }
        UpkeepHolder upkeep = level.getCapability(ServusFoundationCapabilities.UPKEEP, pos, null);
        boolean keeps = upkeep != null && !upkeep.upkeepItem().isEmpty() && upkeep.upkeepLevel() > 0;
        ItemStack upkeepItem = keeps ? upkeep.upkeepItem().copyWithCount(1) : ItemStack.EMPTY;
        int upkeepCount = keeps ? upkeep.upkeepCount() : 0;
        int upkeepLevel = keeps ? upkeep.upkeepLevel() : 0;
        String name = machineName(level, pos);
        boolean noMachine = false;
        if (level.getBlockEntity(pos) instanceof MachineAdapterBlockEntity adapter) {
            BlockState faced = adapter.machineState();
            if (faced != null && !faced.isAir()) {
                icon = new ItemStack(faced.getBlock());
            }
            noMachine = adapter.profile().isEmpty();
        }
        ProductionProvider provider = member.has(DataRole.PRODUCER)
                ? level.getCapability(ServusFoundationCapabilities.PRODUCTION, pos, null) : null;
        List<ProductionRecipe> recipes = provider == null ? List.of() : provider.recipes();
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
        String state;
        if (noMachine) {
            state = "no_machine";
        } else if (!working.isEmpty()) {
            state = "crafting";
        } else if (keeps && upkeepCount <= 0) {
            state = "out_of_fuel";
        } else if (!blockedReason.isEmpty()) {
            state = "blocked";
        } else if (!member.has(DataRole.PRODUCER)) {
            state = "upkeep_only";
        } else if (recipes.isEmpty()) {
            state = "no_recipes";
        } else {
            state = "idle";
        }
        return new MachineRow(pos, icon, name, state, working, 0, state.equals("blocked") ? blockedReason : "", upkeepItem, upkeepCount,
                upkeepLevel, makes(recipes));
    }

    /** How many different items the recipes make. */
    private static int makes(List<ProductionRecipe> recipes) {
        Set<Item> items = new HashSet<>();
        for (ProductionRecipe recipe : recipes) {
            for (ItemStack output : recipe.outputs()) {
                items.add(output.getItem());
            }
        }
        return items.size();
    }

    /** The block's name, or the faced machine's name with "(adapter)" for a Machine Adapter. */
    private static String machineName(ServerLevel level, BlockPos pos) {
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof MachineAdapterBlockEntity adapter) {
            return adapter.machineName().getString() + " (adapter)";
        }
        return name(level, pos);
    }

    private static String name(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock().getName().getString();
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
