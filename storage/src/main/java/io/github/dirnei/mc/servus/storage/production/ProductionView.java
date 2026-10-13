package io.github.dirnei.mc.servus.storage.production;

import io.github.dirnei.mc.servus.foundation.adapter.MachineAdapterBlockEntity;
import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.production.TransferGate;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuBuffer;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.pattern.PatternLibrary;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.BufferItem;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.BufferLine;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.DuctRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.ItemCount;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.JobRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PackingRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternsView;
import io.github.dirnei.mc.servus.storage.terminal.TerminalWarehouses;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

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
            List<CpuBuffer.Entry<ItemKey>> buffered = cpu.buffer().entries();
            cpus.add(cpuRow(cpu, open, buffered));
            for (WorkOrder order : cpu.book().orders()) {
                jobs.add(job(level, cpu.getBlockPos(), order, buffered));
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
        // Order ids grow per CPU, so CPU then id keeps creation order.
        jobs.sort(Comparator.comparing((JobRow job) -> job.open() ? 0 : 1)
                .thenComparingLong(job -> job.cpu().asLong()).thenComparingInt(JobRow::id));
        List<MachineRow> machines = machines(level, line.get(), blocked);
        boolean online = !cpus.isEmpty() || !TerminalWarehouses.reachable(level, pos).isEmpty();
        return new ProductionPayload(pos, online, online ? "" : "nothing", cpus, jobs, machines, patterns(level, line.get()),
                packing(level, line.get()), busiest(level, line.get()));
    }

    /** Every loaded Packing Station of the line with its rate use per direction, by position. */
    public static List<PackingRow> packing(ServerLevel level, DataNetwork line) {
        List<PackingRow> rows = new ArrayList<>();
        for (DataMember member : line.dataMembers(DataRole.LINK)) {
            if (level.isLoaded(member.pos()) && level.getBlockEntity(member.pos()) instanceof PackingStationBlockEntity station) {
                rows.add(new PackingRow(member.pos(), station.used(TransferGate.Flow.IN), station.used(TransferGate.Flow.OUT),
                        (int) Math.round(station.rate())));
            }
        }
        rows.sort(Comparator.comparingLong(row -> row.pos().asLong()));
        return rows;
    }

    /** The data duct of the line with the highest share of its bandwidth used during the last second, if any carried items. */
    public static List<DuctRow> busiest(ServerLevel level, DataNetwork line) {
        return line.busiest().map(load -> {
            String name = level.isLoaded(load.pos()) && level.getBlockEntity(load.pos()) instanceof UtilityDuctBlockEntity duct
                    ? new ItemStack(duct.lineItem(Line.DATA)).getHoverName().getString()
                    : name(level, load.pos());
            return List.of(new DuctRow(load.pos(), name, load.load(), (int) Math.round(load.bandwidth())));
        }).orElse(List.of());
    }

    /** The patterns of the line with their storage and the line's capacity. */
    public static PatternsView patterns(ServerLevel level, DataNetwork line) {
        List<PatternRow> rows = new ArrayList<>();
        for (PatternLibrary.Entry entry : PatternLibrary.entries(level, line)) {
            rows.add(new PatternRow(entry.storage(), entry.slot(), entry.pattern().result(), entry.pattern().inputs()));
        }
        return new PatternsView(rows, rows.size(), PatternLibrary.capacity(level, line));
    }

    /** A CPU with its box, slots and what its buffer holds. */
    private static CpuRow cpuRow(CpuBlockEntity cpu, int open, List<CpuBuffer.Entry<ItemKey>> buffered) {
        int slots = cpu.slots();
        CpuShape.Vec size = cpu.boxSize();
        List<BufferItem> items = new ArrayList<>();
        for (CpuBuffer.Entry<ItemKey> entry : buffered) {
            items.add(new BufferItem(entry.key().stack(1), entry.count(), (int) entry.order(), entry.leaving()));
        }
        return new CpuRow(cpu.getBlockPos(), slots, Math.min(open, slots), Math.max(0, open - slots),
                size == null ? 0 : size.x(), size == null ? 0 : size.y(), size == null ? 0 : size.z(),
                cpu.cpuBlocks(), cpu.bufferBlocks(), cpu.room(), cpu.bufferUsed(), items);
    }

    /** What waits in the buffer for an open order, one line per later step that will use it, in step order. */
    static List<BufferLine> bufferLines(ServerLevel level, WorkOrder order, List<CpuBuffer.Entry<ItemKey>> buffered) {
        if (!order.open()) {
            return List.of();
        }
        Map<OrderStep, List<ItemCount>> byStep = new LinkedHashMap<>();
        for (OrderStep step : order.steps()) {
            byStep.put(step, new ArrayList<>());
        }
        for (CpuBuffer.Entry<ItemKey> entry : buffered) {
            if (entry.order() != order.id() || entry.leaving()) {
                continue;
            }
            for (OrderStep step : order.steps()) {
                if (step.state() != OrderStep.State.DONE && step.inputs().containsKey(entry.key())) {
                    byStep.get(step).add(new ItemCount(entry.key().stack(1), entry.count()));
                    break;
                }
            }
        }
        List<BufferLine> lines = new ArrayList<>();
        byStep.forEach((step, items) -> {
            if (!items.isEmpty()) {
                lines.add(new BufferLine(machineName(level, step.producer()), items));
            }
        });
        return lines;
    }

    private static JobRow job(ServerLevel level, BlockPos cpu, WorkOrder order, List<CpuBuffer.Entry<ItemKey>> buffered) {
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
            String sending = current.handing().map(handing -> handing.key().stack(1).getHoverName().getString()).orElse("");
            String sendingTo = current.handing().map(handing -> machineName(level, handing.target())).orElse("");
            step = new JobStatus.Step(machineName(level, current.producer()), lower(current.state()), current.reason(),
                    verb(level, current.recipe().id()), done, current.batches(), outOfFuel(level, current.producer()),
                    sending, sendingTo, level.isLoaded(current.producer())
                    && level.getBlockEntity(current.producer()) instanceof CraftingStationBlockEntity station && station.waitingForPower());
        }
        JobStatus.Status status = JobStatus.of(state, order.reason(), missingName, missingCount, step);
        List<ItemCount> overflow = new ArrayList<>();
        order.overflowed().forEach((item, count) -> overflow.add(new ItemCount(item.stack(1), count)));
        return new JobRow(cpu, order.id(), key.stack(1), order.amount(), crafted, state, order.open() ? -1 : order.doneAt(),
                status.key(), status.args(), order.requestId(), bufferLines(level, order, buffered), overflow);
    }

    /** crafting, smelting, ... from the recipe's type, working when the recipe is not a known one. */
    private static String verb(ServerLevel level, Identifier recipe) {
        return ServusRecipes.byId(level, recipe)
                .map(holder -> BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType()))
                .map(Identifier::getPath)
                .orElse("working");
    }

    private static boolean outOfFuel(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        UpkeepHolder upkeep = upkeep(level, pos);
        return upkeep != null && upkeep.upkeepCount() <= 0;
    }

    /** The upkeep of the block at the position, or null when it keeps none. */
    private static UpkeepHolder upkeep(ServerLevel level, BlockPos pos) {
        UpkeepHolder upkeep = level.getCapability(ServusFoundationCapabilities.UPKEEP, pos, null);
        return upkeep != null && !upkeep.upkeepItem().isEmpty() && upkeep.upkeepLevel() > 0 ? upkeep : null;
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
            return stationRow(level, pos, icon, station, blockedReason);
        }
        UpkeepHolder upkeep = upkeep(level, pos);
        boolean keeps = upkeep != null;
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
        Optional<Identifier> active = provider == null ? Optional.empty() : provider.active();
        if (active.isPresent()) {
            for (ProductionRecipe recipe : recipes) {
                if (recipe.id().equals(active.get()) && !recipe.outputs().isEmpty()) {
                    working = recipe.outputs().get(0).copyWithCount(1);
                    break;
                }
            }
        }
        String state = machineState(noMachine, !working.isEmpty(), keeps && upkeepCount <= 0, !blockedReason.isEmpty(),
                member.has(DataRole.PRODUCER), !recipes.isEmpty());
        return new MachineRow(pos, icon, name, state, working, 0, state.equals("blocked") ? blockedReason : "", upkeepItem, upkeepCount,
                upkeepLevel, makes(recipes));
    }

    /** A Crafting Station shows only the job it is crafting, no recipe list. */
    private static MachineRow stationRow(ServerLevel level, BlockPos pos, ItemStack icon, CraftingStationBlockEntity station,
                                         String blockedReason) {
        if (station.working()) {
            boolean noPower = station.waitingForPower();
            return new MachineRow(pos, icon, name(level, pos), noPower ? "blocked" : "crafting",
                    station.job().orElseThrow().result().copyWithCount(1), station.jobLeft(), noPower ? "no power" : "",
                    ItemStack.EMPTY, 0, 0, -1);
        }
        String reason = blockedReason.isEmpty() && station.job().isEmpty() && station.hasOutputs() ? "outputs not collected" : blockedReason;
        String state = reason.isEmpty() ? "idle" : "blocked";
        return new MachineRow(pos, icon, name(level, pos), state, ItemStack.EMPTY, 0, reason, ItemStack.EMPTY, 0, 0, -1);
    }

    private static String machineState(boolean noMachine, boolean working, boolean outOfFuel, boolean blocked, boolean producer,
                                       boolean hasRecipes) {
        if (noMachine) {
            return "no_machine";
        }
        if (working) {
            return "crafting";
        }
        if (outOfFuel) {
            return "out_of_fuel";
        }
        if (blocked) {
            return "blocked";
        }
        if (!producer) {
            return "upkeep_only";
        }
        return hasRecipes ? "idle" : "no_recipes";
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
