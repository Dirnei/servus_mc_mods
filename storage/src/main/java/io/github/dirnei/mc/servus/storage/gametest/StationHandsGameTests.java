package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** Crafting Stations as the CPU's hands: pattern jobs, parallel stations, refusals and cancel. */
public final class StationHandsGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos CRATE = new BlockPos(1, 1, 2);
    private static final BlockPos STATION_A = new BlockPos(4, 1, 2);
    private static final BlockPos STATION_B = new BlockPos(5, 1, 2);
    private static final BlockPos PATTERNS = new BlockPos(4, 3, 2);
    private static final BlockPos CPU = new BlockPos(6, 1, 1);
    private static final BlockPos LONE = new BlockPos(10, 1, 10);
    private static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));

    private StationHandsGameTests() {
    }

    /** Warehouse with planks, Packing Station, two plain stations, a sticks pattern and a CPU on one data line. */
    static CrateBlockEntity setup(GameTestHelper helper, int planks) {
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(CRATE, CrateBlockEntity.class);
        crate.insert(new ItemStack(Items.OAK_PLANKS, planks), false);
        TerminalWiring.place(helper, ACCESS);
        for (BlockPos station : List.of(STATION_A, STATION_B)) {
            helper.setBlock(station, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        }
        for (BlockPos pos : List.of(new BlockPos(3, 2, 3), new BlockPos(4, 2, 3), new BlockPos(4, 2, 2), new BlockPos(5, 2, 2))) {
            TerminalWiring.dataDuct(helper, pos);
        }
        helper.setBlock(PATTERNS, ServusStorageBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(PatternStorageBlock.FACING, Direction.NORTH));
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(0, StationJobs.sticks());
        for (int part = 0; part < CpuShape.PARTS; part++) {
            CpuShape.Vec offset = CpuShape.offset(part);
            helper.setBlock(CPU.offset(offset.x(), offset.y(), offset.z()), ServusStorageBlocks.CRAFTING_CPU.get().defaultBlockState());
        }
        helper.assertTrue(ServusStorageBlocks.CRAFTING_CPU.get().tryForm(helper.getLevel(), helper.absolutePos(CPU)), "the cube should form");
        return crate;
    }

    static DataNetwork line(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2))).orElseThrow();
    }

    private static boolean ready(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2)))
                .map(network -> network.dataMembers(DataRole.PRODUCER).size() == 2
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false);
    }

    private static CpuBlockEntity cpu(GameTestHelper helper) {
        return CpuBlock.master(helper.getLevel(), helper.absolutePos(CPU));
    }

    private static CraftingStationBlockEntity station(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, CraftingStationBlockEntity.class);
    }

    /** A station still holding outputs takes no job; the CPU gives the step to the free station instead. */
    @ServusGameTest(timeoutTicks = 800)
    public static void stationWithOutputsIsSkipped(GameTestHelper helper) {
        setup(helper, 8);
        CraftingStationBlockEntity held = station(helper, STATION_A);
        held.outputs().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 3));
        helper.assertTrue(held.blockedFor(new Object()) && !held.assign(StationJobs.sticks(), 1, new Object()),
                "a station with leftovers in its outputs refuses a new job");
        TerminalWiring.when(helper, () -> ready(helper), 60, () -> "stations and CPU should join the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 4, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE, 700,
                    () -> "the order should finish on the free station, state " + order.state() + " " + order.reason(), () -> {
                helper.assertTrue(order.steps().get(0).producer().equals(helper.absolutePos(STATION_B)),
                        "the step should run on the free station, ran on " + order.steps().get(0).producer());
                helper.assertTrue(held.job().isEmpty() && held.outputs().getStackInSlot(0).is(Items.COBBLESTONE),
                        "the blocked station got no job and keeps its leftovers");
                var row = io.github.dirnei.mc.servus.storage.production.ProductionView.of(helper.getLevel(),
                                helper.absolutePos(new BlockPos(4, 2, 2))).machines().stream().filter(machine -> machine.pos().equals(helper.absolutePos(STATION_A))).findFirst();
                helper.assertTrue(row.isPresent() && row.get().state().equals("blocked"),
                        "the Machines tab shows the station as blocked, got " + row.map(r -> r.state()).orElse("none"));
                helper.succeed();
            });
        });
    }

    /** An order from a pattern runs on a station that has nothing configured; it is idle again once the step is done. */
    @ServusGameTest(timeoutTicks = 800)
    public static void patternOrderOnPlainStation(GameTestHelper helper) {
        setup(helper, 8);
        TerminalWiring.when(helper, () -> ready(helper), 60, () -> "stations and CPU should join the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 4, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            BlockPos producer = order.steps().get(0).producer();
            CraftingStationBlockEntity station = helper.getLevel().getBlockEntity(producer) instanceof CraftingStationBlockEntity found ? found : null;
            helper.assertTrue(station != null, "the step runs on a Crafting Station");
            TerminalWiring.when(helper, station::working, 60, () -> "the station should get a job", () -> {
                helper.assertTrue(station.job().map(job -> job.result().is(Items.STICK)).orElse(false) && station.jobLeft() == 1,
                        "a sticks job for one craft, got " + station.jobLeft());
                TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE, 700,
                        () -> "the order should finish, state " + order.state() + " " + order.reason(), () -> {
                    helper.assertTrue(station.job().isEmpty(), "the station is idle after the step");
                    helper.succeed();
                });
            });
        });
    }

    /**
     * A station right beside the Packing Station keeps its job's sticks for the CPU instead of pushing them
     * into the Packing Station, so they are counted and the order finishes.
     */
    @ServusGameTest(timeoutTicks = 800)
    public static void stationBesidePackingStationFinishes(GameTestHelper helper) {
        setup(helper, 8);
        BlockPos beside = new BlockPos(3, 1, 3);
        helper.setBlock(STATION_A, Blocks.AIR.defaultBlockState());
        helper.setBlock(STATION_B, Blocks.AIR.defaultBlockState());
        helper.setBlock(beside, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        TerminalWiring.when(helper, () -> DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2)))
                .map(network -> network.dataMembers(DataRole.PRODUCER).size() == 1
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false), 60, () -> "the station beside the Packing Station and the CPU should join the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 4, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            helper.assertTrue(order.steps().get(0).producer().equals(helper.absolutePos(beside)),
                    "the step should run on the station beside the Packing Station, got " + order.steps().get(0).producer());
            TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE, 700,
                    () -> "the order should finish, state " + order.state() + " " + order.reason(), () -> {
                helper.assertTrue(station(helper, beside).job().isEmpty(), "the station is idle after the step");
                helper.succeed();
            });
        });
    }

    /** Two orders made one after the other run on two different stations at the same time. */
    @ServusGameTest(timeoutTicks = 800)
    public static void twoStationsCraftInParallel(GameTestHelper helper) {
        setup(helper, 16);
        TerminalWiring.when(helper, () -> ready(helper), 60, () -> "stations and CPU should join the line", () -> {
            CpuOrders.Placed first = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            CpuOrders.Placed second = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            helper.assertTrue(first.made() && second.made(), "both orders should be made");
            BlockPos a = first.created().order().steps().get(0).producer();
            BlockPos b = second.created().order().steps().get(0).producer();
            helper.assertTrue(!a.equals(b), "the orders should use different stations, both on " + a);
            CraftingStationBlockEntity stationA = station(helper, STATION_A);
            CraftingStationBlockEntity stationB = station(helper, STATION_B);
            TerminalWiring.when(helper, () -> stationA.working() && stationB.working(), 120,
                    () -> "both stations should have a job at once", () -> {
                TerminalWiring.when(helper, () -> first.created().order().state() == WorkOrder.State.DONE
                                && second.created().order().state() == WorkOrder.State.DONE, 650,
                        () -> "both orders should finish", () -> {
                    helper.assertTrue(stationA.job().isEmpty() && stationB.job().isEmpty(), "both stations are idle again");
                    helper.succeed();
                });
            });
        });
    }

    /** A station without a job refuses a hopper; with a job it takes the planks. */
    @ServusGameTest(timeoutTicks = 200)
    public static void stationWithoutJobRefusesHopper(GameTestHelper helper) {
        helper.setBlock(LONE, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(LONE, CraftingStationBlockEntity.class);
        helper.setBlock(LONE.above(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(LONE.above(), HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 4, "the hopper keeps its planks while the station has no job");
            StationJobs.sticksJob(station, 4);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(hopper.getItem(0).isEmpty(), "with a job the planks go in");
                helper.succeed();
            });
        });
    }

    /** Cancelling the order ends the station's job; its inputs stay. */
    @ServusGameTest(timeoutTicks = 400)
    public static void cancelReturnsStationToIdle(GameTestHelper helper) {
        setup(helper, 0);
        TerminalWiring.when(helper, () -> ready(helper), 60, () -> "stations and CPU should join the line", () -> {
            CrateBlockEntity crate = helper.getBlockEntity(CRATE, CrateBlockEntity.class);
            crate.insert(new ItemStack(Items.OAK_PLANKS, 1), false);
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            OrderStep step = order.steps().get(0);
            CraftingStationBlockEntity station = station(helper, step.producer().equals(helper.absolutePos(STATION_A)) ? STATION_A : STATION_B);
            TerminalWiring.when(helper, () -> station.working() && station.inputs().getStackInSlot(0).is(Items.OAK_PLANKS), 300,
                    () -> "the station should get the job and the one plank", () -> {
                cpu(helper).book().cancel(order.id());
                helper.assertTrue(station.job().isEmpty(), "cancel ends the job");
                helper.assertTrue(station.inputs().getStackInSlot(0).is(Items.OAK_PLANKS), "the plank stays in the inputs");
                helper.succeed();
            });
        });
    }

    /** A station with crafts left refuses another owner's job; the same owner may hand it again. */
    @ServusGameTest(timeoutTicks = 20)
    public static void secondJobIsRefused(GameTestHelper helper) {
        helper.setBlock(LONE, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(LONE, CraftingStationBlockEntity.class);
        Object owner = StationJobs.sticksJob(station, 2);
        EncodedPattern torch = new EncodedPattern(Identifier.withDefaultNamespace("torch"),
                List.of(ItemStack.EMPTY, new ItemStack(Items.COAL), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.STICK)),
                new ItemStack(Items.TORCH, 4));
        helper.assertTrue(!station.assign(torch, 1, new Object()), "another owner's job is refused");
        helper.assertTrue(station.job().map(job -> job.result().is(Items.STICK)).orElse(false), "the sticks job stays");
        helper.assertTrue(station.assign(StationJobs.sticks(), 3, owner) && station.jobLeft() == 3, "the same owner may hand it again");
        station.release(new Object());
        helper.assertTrue(station.working(), "a release from another owner changes nothing");
        station.release(owner);
        helper.assertTrue(station.job().isEmpty(), "the owner's release ends it");
        helper.succeed();
    }
}
