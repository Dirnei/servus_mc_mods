package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.adapter.MachineAdapterBlock;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.production.ProductionPayload;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.JobRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.storage.production.ProductionTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.production.ProductionView;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;

/**
 * The Production Terminal on the data line. Same layout as the CPU tests: the
 * warehouse controller at (2,1,2) with crates west, a Packing Station at (2,1,3)
 * with a data duct and Storage Terminal above, data ducts along y 2 to a
 * Crafting Station at (4,1,2) and a 2x2x2 CPU of 2 CPU and 6 Buffer blocks at x 6 to 7, z 1 to 2. The
 * Production Terminal stands on the duct at (5,2,2).
 */
public final class ProductionTerminalGameTests {

    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    /** The rack west of the rail under the controller. */
    private static final BlockPos RACK = new BlockPos(1, 0, 2);
    private static final BlockPos STATION = new BlockPos(4, 1, 2);
    private static final BlockPos CPU = new BlockPos(6, 1, 1);
    private static final BlockPos TERMINAL = new BlockPos(5, 3, 2);
    private static final BlockPos PATTERNS = new BlockPos(4, 3, 2);
    private static final BlockPos LONE = new BlockPos(10, 1, 10);
    private static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));

    private ProductionTerminalGameTests() {
    }

    private static void setup(GameTestHelper helper, boolean withCpu) {
        MiniWarehouse.under(helper, CONTROLLER);
        TerminalWiring.place(helper, ACCESS);
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(STATION, CraftingStationBlockEntity.class);
        helper.setBlock(PATTERNS, ServusStorageBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(PatternStorageBlock.FACING, Direction.NORTH));
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(0, StationJobs.sticks());
        for (BlockPos pos : List.of(new BlockPos(3, 2, 3), new BlockPos(4, 2, 3), new BlockPos(4, 2, 2), new BlockPos(5, 2, 2))) {
            TerminalWiring.dataDuct(helper, pos);
        }
        if (withCpu) {
            CpuBuild.slotted(helper, CPU);
            helper.assertTrue(CpuBuild.form(helper, CPU).formed(), "the box should form");
        }
        terminal(helper, TERMINAL);
    }

    private static void terminal(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.PRODUCTION_TERMINAL.get().defaultBlockState());
    }

    private static boolean lit(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(StorageNodeBlocks.ONLINE);
    }

    private static ProductionPayload view(GameTestHelper helper, BlockPos pos) {
        return ProductionView.of(helper.getLevel(), helper.absolutePos(pos));
    }

    private static DataNetwork line(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(5, 2, 2))).orElseThrow();
    }

    private static CpuBlockEntity cpu(GameTestHelper helper) {
        return CpuBlock.master(helper.getLevel(), helper.absolutePos(CPU));
    }

    // CPUs tab and buffer lines

    private static final ItemKey PLANKS = ItemKey.of(new ItemStack(Items.OAK_PLANKS));
    private static final ItemKey IRON = ItemKey.of(new ItemStack(Items.IRON_INGOT));

    /** Puts items of an order into the CPU's buffer as if they had arrived. */
    private static void buffer(CpuBlockEntity cpu, long transfer, int order, ItemKey key, int count) {
        cpu.buffer().expect(transfer, order, key, count, key.stack(1).getMaxStackSize());
        cpu.buffer().land(key, count);
        cpu.buffer().arrived(transfer);
    }

    /** A 3x2x2 CPU with 4 slots and 8 stacks runs 2 jobs and holds planks and ingots: its row shows all of it. */
    @ServusGameTest(timeoutTicks = 100)
    public static void cpuRowsShowSlotsAndBuffer(GameTestHelper helper) {
        setup(helper, false);
        java.util.Set<BlockPos> cpus = java.util.Set.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(2, 0, 0), new BlockPos(0, 1, 0));
        CpuBuild.box(helper, CPU, 3, 2, 2, offset -> !cpus.contains(offset));
        CpuBuild.formed(helper, CPU);
        Waits.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40, () -> "the CPU should be on the line", () -> {
            CpuBlockEntity cpu = cpu(helper);
            int first = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0).created().order().id();
            int second = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0).created().order().id();
            buffer(cpu, 1, first, PLANKS, 4);
            buffer(cpu, 2, second, IRON, 2);
            CpuRow row = view(helper, TERMINAL).cpus().get(0);
            helper.assertTrue(row.sizeX() == 3 && row.sizeY() == 2 && row.sizeZ() == 2, "a 3x2x2 box, got " + row);
            helper.assertTrue(row.slots() == 4 && row.inUse() == 2 && row.waiting() == 0, "2 of 4 slots, got " + row);
            helper.assertTrue(row.room() == 8 && row.used() == 2 && row.cpuBlocks() == 4 && row.bufferBlocks() == 8, "2 of 8 stacks, got " + row);
            helper.assertTrue(row.buffered().size() == 2 && row.buffered().get(0).item().is(Items.OAK_PLANKS) && row.buffered().get(0).count() == 4
                            && row.buffered().get(0).order() == first && row.buffered().get(1).item().is(Items.IRON_INGOT)
                            && row.buffered().get(1).count() == 2 && !row.buffered().get(1).leaving(),
                    "planks and ingots with counts, got " + row.buffered());
            helper.succeed();
        });
    }

    /** Planks of a crafting table job in the buffer show as a line for the table step's Crafting Station. */
    @ServusGameTest(timeoutTicks = 100)
    public static void jobShowsBufferedItems(GameTestHelper helper) {
        setup(helper, true);
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(1, StationJobs.planks());
        patterns.set(2, StationJobs.craftingTable());
        Waits.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40, () -> "the CPU should be on the line", () -> {
            CpuBlockEntity cpu = cpu(helper);
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), ItemKey.of(new ItemStack(Items.CRAFTING_TABLE)), 1, 0);
            helper.assertTrue(placed.made() && placed.created().order().steps().size() == 2, "a planks and a table step, refused " + placed.refusal());
            buffer(cpu, 1, placed.created().order().id(), PLANKS, 4);
            JobRow job = view(helper, TERMINAL).jobs().get(0);
            helper.assertTrue(job.buffer().size() == 1, "one buffer line, got " + job.buffer());
            io.github.dirnei.mc.servus.storage.production.ProductionRows.BufferLine line = job.buffer().get(0);
            helper.assertTrue(line.machine().equals(ServusStorageBlocks.CRAFTING_STATION.get().getName().getString())
                            && line.items().size() == 1 && line.items().get(0).item().is(Items.OAK_PLANKS) && line.items().get(0).count() == 4,
                    "In CPU buffer for Crafting Station: 4 Oak Planks, got " + line);
            helper.assertTrue(job.overflow().isEmpty(), "no overflow, got " + job.overflow());
            helper.succeed();
        });
    }

    /** Planks that went to storage because the buffer was full show as an overflow line under the job. */
    @ServusGameTest(timeoutTicks = 100)
    public static void jobShowsOverflow(GameTestHelper helper) {
        setup(helper, true);
        Waits.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40, () -> "the CPU should be on the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            placed.created().order().overflowed(PLANKS, 16);
            JobRow job = view(helper, TERMINAL).jobs().get(0);
            helper.assertTrue(job.overflow().size() == 1 && job.overflow().get(0).item().is(Items.OAK_PLANKS) && job.overflow().get(0).count() == 16,
                    "Buffer full: 16 Oak Planks went to storage, got " + job.overflow());
            helper.succeed();
        });
    }

    /** Without a formed CPU the view has no CPU rows, so the CPUs tab says no CPU is on the line. */
    @ServusGameTest(timeoutTicks = 60)
    public static void cpusTabWithoutCpu(GameTestHelper helper) {
        setup(helper, false);
        helper.runAfterDelay(20, () -> {
            ProductionPayload view = view(helper, TERMINAL);
            helper.assertTrue(view.cpus().isEmpty(), "no CPU rows, got " + view.cpus());
            helper.succeed();
        });
    }

    /** On a line with a CPU the terminal is a link member and its lamp lights. */
    @ServusGameTest(timeoutTicks = 100)
    public static void productionTerminalJoinsLineAndLights(GameTestHelper helper) {
        setup(helper, true);
        Waits.when(helper, () -> lit(helper, TERMINAL), 60, () -> "the lamp should light", () -> {
            helper.assertTrue(line(helper).dataMembers(DataRole.LINK).stream()
                    .anyMatch(member -> member.pos().equals(helper.absolutePos(TERMINAL))), "the terminal should be a link member");
            helper.succeed();
        });
    }

    /** Alone the lamp stays dark and the view says it is not on a line. */
    @ServusGameTest(timeoutTicks = 60)
    public static void productionTerminalAloneIsDark(GameTestHelper helper) {
        terminal(helper, LONE);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(!lit(helper, LONE), "the lamp should stay dark");
            ProductionPayload view = view(helper, LONE);
            helper.assertTrue(!view.online() && view.reason().equals("nothing"), "offline with nothing on its own line, got " + view.reason());
            helper.succeed();
        });
    }

    /** An idle Crafting Station is a status row: idle, any pattern, no recipe list; with a job it is crafting with crafts left. */
    @ServusGameTest(timeoutTicks = 60)
    public static void productionTerminalListsIdleStation(GameTestHelper helper) {
        setup(helper, false);
        Waits.when(helper, () -> !view(helper, TERMINAL).machines().isEmpty(), 40, () -> "the station should be listed", () -> {
            MachineRow machine = view(helper, TERMINAL).machines().get(0);
            helper.assertTrue(machine.pos().equals(helper.absolutePos(STATION)) && machine.state().equals("idle"),
                    "the station should be idle, got " + machine.state());
            helper.assertTrue(machine.makes() == -1 && machine.icon().is(ServusStorageBlocks.CRAFTING_STATION.get().asItem()),
                    "any pattern with the station icon, got " + machine);
            CraftingStationBlockEntity station = helper.getBlockEntity(STATION, CraftingStationBlockEntity.class);
            StationJobs.sticksJob(station, 2);
            MachineRow working = view(helper, TERMINAL).machines().get(0);
            helper.assertTrue(working.state().equals("crafting") && working.item().is(Items.STICK) && working.left() == 2,
                    "crafting sticks, 2 left, got " + working.state() + " " + working.left());
            helper.succeed();
        });
    }

    /** A CPU with a job shows the slot summary and one open job row for 8 sticks with a status. */
    @ServusGameTest(timeoutTicks = 100)
    public static void productionTerminalListsCpuJob(GameTestHelper helper) {
        setup(helper, true);
        Waits.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40, () -> "the CPU should be on the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            ProductionPayload view = view(helper, TERMINAL);
            helper.assertTrue(view.online() && view.cpus().size() == 1, "one CPU row, got " + view.cpus());
            CpuRow row = view.cpus().get(0);
            helper.assertTrue(row.cpu().equals(cpu(helper).getBlockPos()) && row.slots() == 2 && row.inUse() == 1 && row.waiting() == 0,
                    "1 of 2 slots, 0 waiting, got " + row);
            helper.assertTrue(view.jobs().size() == 1, "one job, got " + view.jobs());
            JobRow job = view.jobs().get(0);
            helper.assertTrue(job.item().is(Items.STICK) && job.amount() == 8 && job.crafted() == 0 && job.open() && job.requestId() == 0,
                    "an open manual job for 8 sticks, got " + job);
            helper.assertTrue(!job.status().isEmpty() && job.doneAt() == -1, "a status and no end time, got " + job);
            helper.succeed();
        });
    }

    /** A job that runs says "Crafting Station: crafting n of 2" and ends done with a full bar. */
    @ServusGameTest(timeoutTicks = 600)
    public static void productionTerminalJobWording(GameTestHelper helper) {
        setup(helper, true);
        WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.OAK_PLANKS, 4));
        Waits.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40, () -> "the CPU should be on the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            boolean[] sawWorking = {false};
            java.util.Set<String> seen = new java.util.LinkedHashSet<>();
            Waits.when(helper, () -> {
                JobRow job = view(helper, TERMINAL).jobs().get(0);
                seen.add(job.status() + job.args());
                if (job.status().equals("working")) {
                    helper.assertTrue(job.args().size() == 4 && job.args().get(1).equals("crafting") && job.args().get(3).equals("2"),
                            "Crafting Station: crafting n of 2, got " + job.args());
                    sawWorking[0] = true;
                }
                return job.state().equals("done");
            }, 500, () -> "the job should finish, got " + view(helper, TERMINAL).jobs(), () -> {
                JobRow done = view(helper, TERMINAL).jobs().get(0);
                helper.assertTrue(sawWorking[0], "the job should have shown the crafting step, saw " + seen);
                helper.assertTrue(done.status().equals("done") && done.crafted() == 8 && done.doneAt() >= 0 && !done.open(),
                        "done with a full bar and an end time, got " + done);
                helper.succeed();
            });
        });
    }

    /** A third job on a CPU with 2 slots waits for a CPU. */
    @ServusGameTest(timeoutTicks = 100)
    public static void productionTerminalWaitingForCpu(GameTestHelper helper) {
        setup(helper, true);
        Waits.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40, () -> "the CPU should be on the line", () -> {
            for (int i = 0; i < 3; i++) {
                helper.assertTrue(CpuOrders.create(helper.getLevel(), line(helper), STICK, 4, 0).made(), "order " + i + " should be made");
            }
            List<JobRow> jobs = view(helper, TERMINAL).jobs();
            helper.assertTrue(jobs.size() == 3, "three jobs, got " + jobs);
            helper.assertTrue(jobs.stream().filter(job -> job.status().equals("waiting_cpu")).count() == 1,
                    "one job waits for a CPU, got " + jobs.stream().map(JobRow::status).toList());
            helper.succeed();
        });
    }

    /** A furnace adapter without fuel is listed first as out of fuel, with a count instead of a recipe list. */
    @ServusGameTest(timeoutTicks = 100)
    public static void productionTerminalMachinesProblemsFirst(GameTestHelper helper) {
        setup(helper, false);
        BlockPos furnace = new BlockPos(5, 1, 1);
        BlockPos adapter = new BlockPos(5, 1, 2);
        helper.setBlock(furnace, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.WEST));
        helper.setBlock(adapter, AdapterContent.BLOCK.get().defaultBlockState().setValue(MachineAdapterBlock.FACING, Direction.NORTH));
        Waits.when(helper, () -> view(helper, TERMINAL).machines().size() == 2, 60, () -> "station and adapter should be listed", () -> {
            List<MachineRow> machines = view(helper, TERMINAL).machines();
            MachineRow first = machines.get(0);
            helper.assertTrue(first.pos().equals(helper.absolutePos(adapter)) && first.state().equals("out_of_fuel") && first.problem(),
                    "the empty furnace adapter should come first as out of fuel, got " + machines);
            helper.assertTrue(first.name().endsWith("(adapter)") && first.icon().is(Items.FURNACE), "named and shown as the furnace, got " + first);
            helper.assertTrue(first.makes() > 10 && first.upkeep().is(Items.COAL) && first.upkeepCount() == 0 && first.upkeepLevel() > 0,
                    "a count of smeltable items and an empty coal upkeep, got " + first);
            helper.assertTrue(machines.get(1).state().equals("idle"), "the station follows as idle, got " + machines.get(1));
            helper.succeed();
        });
    }

    /** Cancelling through the terminal cancels the order on its CPU and releases its reservations. */
    @ServusGameTest(timeoutTicks = 100)
    public static void productionTerminalCancelsOrder(GameTestHelper helper) {
        setup(helper, true);
        WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.OAK_PLANKS, 10));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Waits.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40, () -> "the CPU should be on the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made");
            WorkOrder order = placed.created().order();
            ProductionTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL, ProductionTerminalBlockEntity.class);
            ProductionTerminalMenu menu = new ProductionTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.cancel(placed.cpu(), order.id()), "the cancel should go through");
            helper.assertTrue(order.state() == WorkOrder.State.CANCELLED, "the order should be cancelled, got " + order.state());
            helper.assertTrue(cpu(helper).book().reservedTotal().isEmpty(), "reservations released, got " + cpu(helper).book().reservedTotal());
            helper.succeed();
        });
    }

    /** A line with a warehouse but no CPU is online and has no CPU rows. */
    @ServusGameTest(timeoutTicks = 100)
    public static void productionTerminalNoCpuRows(GameTestHelper helper) {
        setup(helper, false);
        Waits.when(helper, () -> lit(helper, TERMINAL), 60, () -> "the warehouse should light the lamp", () -> {
            ProductionPayload view = view(helper, TERMINAL);
            helper.assertTrue(view.online() && view.cpus().isEmpty(), "online without CPU rows, got " + view.cpus());
            helper.succeed();
        });
    }

    /** A Crafting Station and a Pattern Storage on a line with a CPU light their online lamps. */
    @ServusGameTest(timeoutTicks = 100)
    public static void stationAndPatternStorageLampsLight(GameTestHelper helper) {
        setup(helper, true);
        Waits.when(helper, () -> lit(helper, STATION) && lit(helper, PATTERNS), 60,
                () -> "station and pattern storage lamps should light", helper::succeed);
    }

    /** Alone, a Crafting Station and a Pattern Storage keep their lamps dark. */
    @ServusGameTest(timeoutTicks = 60)
    public static void stationAndPatternStorageAloneDark(GameTestHelper helper) {
        BlockPos station = LONE;
        BlockPos patterns = LONE.east(1).south(0).west(2);
        helper.setBlock(station, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        helper.setBlock(patterns, ServusStorageBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(PatternStorageBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(!lit(helper, station), "the station lamp should stay dark");
            helper.assertTrue(!lit(helper, patterns), "the pattern storage lamp should stay dark");
            helper.succeed();
        });
    }
}
