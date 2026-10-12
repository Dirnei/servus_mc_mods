package io.github.dirnei.mc.servus.storage.gametest;

import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.adapter.MachineAdapterBlock;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;

/** Intermediates of a chain go straight from one producer to the next, or wait in the CPU buffer, instead of going through storage. */
public final class ChainHandoffGameTests {

    private static final String EMPTY = "empty_12x6x12";
    /** A second station beside the first, under the duct at (5, 2, 2). */
    private static final BlockPos STATION_B = new BlockPos(5, 1, 2);
    private static final ItemKey STONE = ItemKey.of(new ItemStack(Items.STONE));
    private static final ItemKey STONE_BRICKS = ItemKey.of(new ItemStack(Items.STONE_BRICKS));

    private ChainHandoffGameTests() {
    }

    /** The CPU test line with a second Crafting Station and the planks pattern next to the sticks pattern. */
    private static void twoStations(GameTestHelper helper) {
        CraftingCpuGameTests.setup(helper, true);
        helper.setBlock(STATION_B, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.NORTH));
        PatternStorageBlockEntity patterns = helper.getBlockEntity(CraftingCpuGameTests.PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(1, StationJobs.planks());
        CraftingCpuGameTests.stock(helper, CraftingCpuGameTests.RACK_A, Items.OAK_LOG, 2);
    }

    private static boolean twoProducers(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2)))
                .map(network -> network.dataMembers(DataRole.PRODUCER).size() == 2
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false);
    }

    /** The most of the key the warehouse held at any tick so far, kept in {@code seen[0]}. */
    private static int[] watch(GameTestHelper helper, BlockPos controller, ItemKey key) {
        int[] seen = {0};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(controller, BlockEntity.class) instanceof WarehouseControllerBlockEntity block) {
                block.network().ifPresent(network -> seen[0] = Math.max(seen[0], (int) network.available(key)));
            }
        });
        return seen;
    }

    /** Waits until a shuttle has put {@code count} sticks away, then succeeds. */
    private static void putAway(GameTestHelper helper, int count) {
        TerminalWiring.when(helper, () -> CraftingCpuGameTests.warehouse(helper).available(CraftingCpuGameTests.STICK) == count,
                MiniWarehouse.TRIP_TICKS * 2, () -> count + " sticks should be put away, warehouse has "
                        + CraftingCpuGameTests.warehouse(helper).available(CraftingCpuGameTests.STICK), helper::succeed);
    }

    private static String steps(WorkOrder order) {
        return order.state() + " " + order.reason() + " " + order.steps().stream()
                .map(step -> step.output().getItem() + "@" + step.producer().toShortString() + ":" + step.state() + "/" + step.reason())
                .toList();
    }

    /** Planks from one station go straight to the second station's sticks job; the warehouse never holds planks. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void planksHandedToSecondStation(GameTestHelper helper) {
        twoStations(helper);
        int[] planks = watch(helper, CraftingCpuGameTests.CONTROLLER, CraftingCpuGameTests.PLANKS);
        WorkOrder[] placedOrder = {null};
        boolean[] handed = {false};
        helper.onEachTick(() -> handed[0] |= placedOrder[0] != null && placedOrder[0].steps().get(0).handing().isPresent());
        TerminalWiring.when(helper, () -> twoProducers(helper), 60, () -> "both stations and the CPU should join the line", () -> {
            DataNetwork line = CraftingCpuGameTests.line(helper);
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line, CraftingCpuGameTests.STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            placedOrder[0] = order;
            TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE, 1300, () -> "the order should finish, " + steps(order), () -> {
                List<OrderStep> steps = order.steps();
                helper.assertTrue(!steps.get(0).producer().equals(steps.get(1).producer()), "the steps should run on two stations");
                helper.assertTrue(handed[0], "the planks step should hand off");
                helper.assertTrue(planks[0] == 0, "no planks should enter the warehouse, saw " + planks[0]);
                putAway(helper, 8);
            });
        });
    }

    /** Stone smelted by a furnace adapter goes straight to a station that makes stone bricks. */
    @ServusGameTest(timeoutTicks = 2400)
    public static void stoneHandedFromFurnaceToStation(GameTestHelper helper) {
        BlockPos controller = new BlockPos(2, 1, 2);
        helper.setBlock(controller, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        PowerFixtures.fill(helper, controller);
        // Rails at y 0 under the controller and the Packing Station, two racks west of them, a garage north.
        BlockPos access = new BlockPos(2, 1, 3);
        for (BlockPos rail : List.of(controller.below(), access.below())) {
            TerminalWiring.rail(helper, rail);
        }
        BlockPos cobbleRack = new BlockPos(1, 0, 2);
        BlockPos coalRack = new BlockPos(1, 0, 3);
        for (BlockPos pos : List.of(cobbleRack, coalRack)) {
            helper.setBlock(pos, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.EAST));
        }
        MiniWarehouse.stock(helper, cobbleRack, new ItemStack(Items.COBBLESTONE, 16));
        MiniWarehouse.stock(helper, coalRack, new ItemStack(Items.COAL, 32));
        BlockPos garagePos = new BlockPos(2, 0, 1);
        helper.setBlock(garagePos, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        helper.assertTrue(helper.getBlockEntity(garagePos, ShuttleGarageBlockEntity.class).addShuttle(), "the garage takes a shuttle");
        TerminalWiring.place(helper, access);
        helper.setBlock(new BlockPos(5, 1, 3), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH));
        helper.setBlock(new BlockPos(4, 1, 3), AdapterContent.BLOCK.get().defaultBlockState().setValue(MachineAdapterBlock.FACING, Direction.EAST));
        BlockPos line = new BlockPos(4, 2, 3);
        for (BlockPos pos : List.of(new BlockPos(3, 2, 3), line, new BlockPos(5, 2, 3))) {
            TerminalWiring.dataDuct(helper, pos);
        }
        helper.setBlock(new BlockPos(3, 1, 3), ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.NORTH));
        BlockPos patternsPos = new BlockPos(4, 3, 3);
        helper.setBlock(patternsPos, ServusStorageBlocks.PATTERN_STORAGE.get().defaultBlockState()
                .setValue(PatternStorageBlock.FACING, Direction.NORTH));
        PatternStorageBlockEntity patterns = helper.getBlockEntity(patternsPos, PatternStorageBlockEntity.class);
        ItemStack stone = new ItemStack(Items.STONE);
        patterns.set(0, new EncodedPattern(Identifier.withDefaultNamespace("stone_bricks"),
                List.of(stone, stone, ItemStack.EMPTY, stone, stone), new ItemStack(Items.STONE_BRICKS, 4)));
        BlockPos cpuPos = new BlockPos(6, 1, 1);
        CpuBuild.cube(helper, cpuPos);
        helper.assertTrue(CpuBuild.form(helper, cpuPos).formed(), "the box should form");
        int[] seen = watch(helper, controller, STONE);
        TerminalWiring.when(helper, () -> DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(line))
                .map(network -> network.dataMembers(DataRole.PRODUCER).size() == 2
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1).orElse(false), 60,
                () -> "the adapter, the station and the CPU should join the line", () -> {
            DataNetwork network = DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(line)).orElseThrow();
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), network, STONE_BRICKS, 4, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            helper.assertTrue(order.steps().size() == 2, "a smelt step and a craft step, got " + steps(order));
            TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE, 2300, () -> "the order should finish, " + steps(order), () -> {
                helper.assertTrue(seen[0] == 0, "no stone should enter the warehouse, saw " + seen[0]);
                helper.succeed();
            });
        });
    }

    // Buffer

    /**
     * Oak logs to a crafting table on one Crafting Station: the planks wait in
     * the CPU's buffer, never show up at the Packing Station or in the
     * warehouse, and the crafting table is made.
     */
    @ServusGameTest(timeoutTicks = 1400)
    public static void chainOnOneStationUsesBuffer(GameTestHelper helper) {
        CpuBlockEntity cpu = CraftingCpuGameTests.bufferedSetup(helper, 6, 1);
        int[] planks = CraftingCpuGameTests.watchStorage(helper, Items.OAK_PLANKS);
        int[] buffered = {0};
        helper.onEachTick(() -> buffered[0] = Math.max(buffered[0], cpu.bufferUsed()));
        TerminalWiring.when(helper, () -> CraftingCpuGameTests.lineReady(helper), 60, () -> "the station and the CPU should join the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), CraftingCpuGameTests.line(helper), CraftingCpuGameTests.CRAFTING_TABLE, 1, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            helper.assertTrue(order.steps().size() == 2 && order.steps().get(0).producer().equals(order.steps().get(1).producer()),
                    "a planks step and a table step on the one station, got " + steps(order));
            TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE
                            && CraftingCpuGameTests.warehouse(helper).available(CraftingCpuGameTests.CRAFTING_TABLE) == 1,
                    1300, () -> "the crafting table should be made and put away, " + steps(order), () -> {
                        helper.assertTrue(buffered[0] > 0, "the planks should wait in the buffer");
                        helper.assertTrue(planks[0] == 0, "no planks at the Packing Station or in the warehouse, saw " + planks[0]);
                        helper.assertTrue(order.overflowed().isEmpty(), "nothing overflowed: " + order.overflowed());
                        helper.assertTrue(cpu.bufferUsed() == 0, "the buffer is empty again");
                        helper.succeed();
                    });
        });
    }

    /** The second station stays busy past the wait: with buffer room the planks wait in the buffer, not at the station or in storage. */
    @ServusGameTest(timeoutTicks = 1600)
    public static void busyProducerUsesBuffer(GameTestHelper helper) {
        CraftingCpuGameTests.setup(helper, false);
        CpuBuild.formSlotted(helper, CraftingCpuGameTests.CPU_A);
        helper.setBlock(STATION_B, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.NORTH));
        PatternStorageBlockEntity patterns = helper.getBlockEntity(CraftingCpuGameTests.PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(1, StationJobs.planks());
        CraftingCpuGameTests.stock(helper, CraftingCpuGameTests.RACK_A, Items.OAK_LOG, 2);
        CraftingStationBlockEntity second = helper.getBlockEntity(STATION_B, CraftingStationBlockEntity.class);
        Object owner = StationJobs.sticksJob(second, 1);
        int[] planks = watch(helper, CraftingCpuGameTests.CONTROLLER, CraftingCpuGameTests.PLANKS);
        TerminalWiring.when(helper, () -> twoProducers(helper), 60, () -> "both stations and the CPU should join the line", () -> {
            CpuBlockEntity cpu = CraftingCpuGameTests.cpu(helper, CraftingCpuGameTests.CPU_A);
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), CraftingCpuGameTests.line(helper), CraftingCpuGameTests.STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            TerminalWiring.when(helper, () -> cpu.buffer().held(order.id(), CraftingCpuGameTests.PLANKS) >= 4, 900,
                    () -> "the planks should wait in the buffer, " + steps(order), () -> {
                        // Well past the wait: still in the buffer, nothing held at the station, nothing in storage.
                        helper.runAfterDelay(StorageStatsManager.INSTANCE.handoffWaitTicks() + 60, () -> {
                            helper.assertTrue(cpu.buffer().held(order.id(), CraftingCpuGameTests.PLANKS) >= 4,
                                    "the planks stay in the buffer, " + steps(order));
                            helper.assertTrue(planks[0] == 0, "no planks in storage, saw " + planks[0]);
                            helper.assertTrue(order.overflowed().isEmpty(), "nothing overflowed: " + order.overflowed());
                            second.release(owner);
                            TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE, 900, () -> "the order should finish, " + steps(order), () -> {
                                helper.assertTrue(planks[0] == 0, "no planks ever in storage, saw " + planks[0]);
                                putAway(helper, 8);
                            });
                        });
                    });
        });
    }

    /**
     * One stack of room and 80 planks for 20 crafting tables on one station:
     * 64 planks wait in the buffer, 16 go to storage, the order notes it and
     * still completes.
     */
    @ServusGameTest(timeoutTicks = 3600)
    public static void fullBufferOverflowsToStorage(GameTestHelper helper) {
        CpuBlockEntity cpu = CraftingCpuGameTests.bufferedSetup(helper, 1, 20);
        int[] most = {0};
        helper.onEachTick(() -> {
            if (cpu.buffer() != null) {
                for (var entry : cpu.buffer().entries()) {
                    most[0] = Math.max(most[0], entry.count());
                }
            }
        });
        TerminalWiring.when(helper, () -> CraftingCpuGameTests.lineReady(helper), 60, () -> "the station and the CPU should join the line", () -> {
            helper.assertTrue(cpu.room() == 1, "one stack of room, got " + cpu.room());
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), CraftingCpuGameTests.line(helper), CraftingCpuGameTests.CRAFTING_TABLE, 20, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE
                            && CraftingCpuGameTests.warehouse(helper).available(CraftingCpuGameTests.CRAFTING_TABLE) == 20,
                    3400, () -> "20 crafting tables should be made, " + steps(order) + " overflow " + order.overflowed(), () -> {
                        helper.assertTrue(most[0] == 64, "the buffer held one full stack, most " + most[0]);
                        helper.assertTrue(order.overflowed().getOrDefault(CraftingCpuGameTests.PLANKS, 0) == 16,
                                "16 planks overflowed, got " + order.overflowed());
                        helper.assertTrue(CraftingCpuGameTests.warehouse(helper).available(CraftingCpuGameTests.PLANKS) == 0,
                                "the stored planks were used");
                        helper.succeed();
                    });
        });
    }

    /** Without Buffer blocks the second station is held by another job past the wait: the planks go to storage and the order still finishes. */
    @ServusGameTest(timeoutTicks = 1600)
    public static void busyNextProducerFallsBack(GameTestHelper helper) {
        twoStations(helper);
        CraftingStationBlockEntity second = helper.getBlockEntity(STATION_B, CraftingStationBlockEntity.class);
        Object owner = StationJobs.sticksJob(second, 1);
        int[] planks = watch(helper, CraftingCpuGameTests.CONTROLLER, CraftingCpuGameTests.PLANKS);
        TerminalWiring.when(helper, () -> twoProducers(helper), 60, () -> "both stations and the CPU should join the line", () -> {
            DataNetwork line = CraftingCpuGameTests.line(helper);
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line, CraftingCpuGameTests.STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            TerminalWiring.when(helper, () -> planks[0] > 0, 900, () -> "the planks should go to storage after the wait, " + steps(order), () -> {
                second.release(owner);
                TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE, 600, () -> "the order should finish, " + steps(order), () -> {
                    putAway(helper, 8);
                });
            });
        });
    }

    /** CPU and both stations are reloaded while planks travel between the stations; nothing is lost or counted twice. */
    @ServusGameTest(timeoutTicks = 1600)
    public static void reloadMidHandoffCompletes(GameTestHelper helper) {
        twoStations(helper);
        TerminalWiring.when(helper, () -> twoProducers(helper), 60, () -> "both stations and the CPU should join the line", () -> {
            DataNetwork line = CraftingCpuGameTests.line(helper);
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line, CraftingCpuGameTests.STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder before = placed.created().order();
            int id = before.id();
            TerminalWiring.when(helper, () -> before.steps().get(0).handing().isPresent(), 900, () -> "a handoff should start, " + steps(before), () -> {
                CpuBlockEntity cpu = CraftingCpuGameTests.cpu(helper, CraftingCpuGameTests.CPU_A);
                BlockPos cpuRel = cpu.getBlockPos().subtract(helper.absolutePos(BlockPos.ZERO));
                CpuBlockEntity loaded = PersistGameTests.reload(helper, cpuRel);
                PersistGameTests.reload(helper, CraftingCpuGameTests.STATION);
                PersistGameTests.reload(helper, STATION_B);
                WorkOrder order = loaded.book().order(id);
                helper.assertTrue(order != null, "the order should survive the reload");
                TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE, 1300, () -> "the order should finish, " + steps(order), () -> {
                    long planks = CraftingCpuGameTests.warehouse(helper).available(CraftingCpuGameTests.PLANKS);
                    helper.assertTrue(planks == 0, "no planks should be left over in storage, has " + planks);
                    putAway(helper, 8);
                });
            });
        });
    }
}
