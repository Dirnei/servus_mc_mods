package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorGameTests;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.production.ProductionPayload;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionView;
import java.util.List;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rail.ElevatorBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMachineKinds;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Numbers of the named machines beyond the stations: elevator shafts, Field Heads and the data line's machines. */
public final class MachineNumberGameTests {

    private static final int WAIT = 120;
    private static final UUID WAREHOUSE = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final BlockPos CONTROLLER = new BlockPos(1, 1, 2);
    private static final BlockPos LIFT_BOTTOM = new BlockPos(4, 1, 3);
    private static final BlockPos COLUMN = new BlockPos(2, 1, 2);
    private static final BlockPos SECOND_STATION = new BlockPos(5, 1, 2);

    private MachineNumberGameTests() {
    }

    private static void whenNumbered(GameTestHelper helper, Supplier<? extends NamedMachine> machine, Runnable then) {
        Waits.when(helper, () -> machine.get().number() > 0, WAIT, () -> "the machine should get a number", then);
    }

    private static void assertNumber(GameTestHelper helper, NamedMachine machine, int number) {
        helper.assertTrue(machine.number() == number, "expected #" + number + ", got #" + machine.number());
    }

    private static Object[] labelArgs(GameTestHelper helper, Component label, String key) {
        helper.assertTrue(label.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                "expected the label " + key + ", got " + label);
        return ((TranslatableContents) label.getContents()).getArgs();
    }

    private static ElevatorBlockEntity segment(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        return helper.getBlockEntity(pos, ElevatorBlockEntity.class);
    }

    /** A three segment shaft on rails of a warehouse is Lift #1 once: every segment reads Lift #1, only the lowest holds it. */
    @ServusGameTest(timeoutTicks = 200)
    public static void shaftNumberedOnce(GameTestHelper helper) {
        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        }
        ElevatorBlockEntity bottom = segment(helper, LIFT_BOTTOM);
        ElevatorBlockEntity middle = segment(helper, LIFT_BOTTOM.above());
        ElevatorBlockEntity top = segment(helper, LIFT_BOTTOM.above(2));
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, CONTROLLER);
        whenNumbered(helper, () -> bottom, () -> helper.runAfterDelay(10, () -> {
            assertNumber(helper, bottom, 1);
            assertNumber(helper, middle, 0);
            assertNumber(helper, top, 0);
            Object[] args = labelArgs(helper, top.label(), "machine.servus_shuttles.elevator");
            helper.assertTrue(args[0].equals(1), "the top segment reads Lift #1, got " + args[0]);
            helper.assertTrue(controller.lastNumber(ServusShuttlesMachineKinds.ELEVATOR) == 1, "the shaft took one number");
            helper.succeed();
        }));
    }

    /** The lowest segment of "Ore Lift" replaced by stone hands number and name to the segment above. */
    @ServusGameTest
    public static void lowestSegmentRemovedKeepsLabel(GameTestHelper helper) {
        helper.setBlock(COLUMN.below(), Blocks.STONE.defaultBlockState());
        ElevatorBlockEntity bottom = segment(helper, COLUMN);
        segment(helper, COLUMN.above());
        segment(helper, COLUMN.above(2));
        bottom.assignNumber(WAREHOUSE, 1);
        bottom.rename("Ore Lift");
        helper.setBlock(COLUMN, Blocks.STONE.defaultBlockState());
        ElevatorBlockEntity next = helper.getBlockEntity(COLUMN.above(), ElevatorBlockEntity.class);
        helper.assertTrue(next.number() == 1 && next.name().equals("Ore Lift"), "the segment above is Ore Lift (#1), got #" + next.number() + " " + next.name());
        helper.assertTrue(helper.getBlockEntity(COLUMN.above(2), ElevatorBlockEntity.class).label().getString().startsWith("Ore Lift"),
                "the top segment reads Ore Lift");
        helper.succeed();
    }

    /** A segment placed under a shaft becomes its lowest one and takes number and name over. */
    @ServusGameTest
    public static void segmentPlacedBelowTakesLabel(GameTestHelper helper) {
        helper.setBlock(COLUMN.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(COLUMN, Blocks.STONE.defaultBlockState());
        ElevatorBlockEntity old = segment(helper, COLUMN.above());
        segment(helper, COLUMN.above(2));
        old.assignNumber(WAREHOUSE, 2);
        old.rename("Ore Lift");
        ElevatorBlockEntity lowest = segment(helper, COLUMN);
        helper.assertTrue(lowest.number() == 2 && lowest.name().equals("Ore Lift"), "the new lowest segment is Ore Lift (#2), got #"
                + lowest.number() + " " + lowest.name());
        helper.assertTrue(old.number() == 0 && old.name().isEmpty(), "the old lowest segment holds nothing");
        helper.succeed();
    }

    /** A segment joining Lift #1 below and "Ore Lift" (#2) above makes one shaft that reads Lift #1. */
    @ServusGameTest
    public static void joinedShaftsKeepTheLowerLabel(GameTestHelper helper) {
        helper.setBlock(COLUMN.below(), Blocks.STONE.defaultBlockState());
        ElevatorBlockEntity lower = segment(helper, COLUMN);
        helper.setBlock(COLUMN.above(), Blocks.STONE.defaultBlockState());
        ElevatorBlockEntity upper = segment(helper, COLUMN.above(2));
        lower.assignNumber(WAREHOUSE, 1);
        upper.assignNumber(WAREHOUSE, 2);
        upper.rename("Ore Lift");
        segment(helper, COLUMN.above());
        helper.assertTrue(lower.number() == 1 && lower.name().isEmpty(), "the lower shaft keeps Lift #1");
        helper.assertTrue(upper.number() == 0 && upper.name().isEmpty(), "the upper shaft's name and number are gone");
        Object[] args = labelArgs(helper, upper.label(), "machine.servus_shuttles.elevator");
        helper.assertTrue(args[0].equals(1), "the joined shaft reads Lift #1, got " + args[0]);
        helper.succeed();
    }

    /** The farm's Field Head becomes Field Head #1; a head broken and placed again gets #2, the number is not reused. */
    @ServusGameTest(timeoutTicks = 300)
    public static void fieldHeadsNumberedByTheirWarehouse(GameTestHelper helper) {
        FarmFixtures.Farm farm = FarmFixtures.farm(helper, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        whenNumbered(helper, farm::head, () -> {
            assertNumber(helper, farm.head(), 1);
            labelArgs(helper, farm.head().label(), "machine.servus_shuttles.field_head");
            helper.setBlock(FarmFixtures.HEAD, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(3, () -> {
                FieldHeadBlockEntity again = FieldFixtures.head(helper, FarmFixtures.HEAD);
                whenNumbered(helper, () -> again, () -> {
                    assertNumber(helper, again, 2);
                    helper.succeed();
                });
            });
        });
    }

    /** On the CPU line the Crafting Station and the CPU become #1; a second station cabled in becomes Crafting Station #2. */
    @ServusGameTest(timeoutTicks = 300)
    public static void dataLineMachinesNumbered(GameTestHelper helper) {
        CpuLine.setup(helper, true);
        CraftingStationBlockEntity station = helper.getBlockEntity(CpuLine.STATION, CraftingStationBlockEntity.class);
        CpuBlockEntity cpu = CpuLine.cpu(helper, CpuLine.CPU_A);
        whenNumbered(helper, () -> station, () -> whenNumbered(helper, () -> cpu, () -> {
            assertNumber(helper, station, 1);
            assertNumber(helper, cpu, 1);
            CpuBlockEntity part = helper.getBlockEntity(CpuLine.CPU_A.offset(1, 1, 1), CpuBlockEntity.class);
            helper.assertTrue(part.number() == 0, "only the master holds the CPU's number");
            Object[] args = labelArgs(helper, part.label(), "machine.servus_shuttles.crafting_cpu");
            helper.assertTrue(args[0].equals(1), "every block of the box reads CPU #1, got " + args[0]);
            CraftingStationBlockEntity second = StationJobs.craftingStation(helper, SECOND_STATION);
            whenNumbered(helper, () -> second, () -> {
                assertNumber(helper, second, 2);
                helper.succeed();
            });
        }));
    }

    /**
     * A Coal Generator and a Packing Station that are only on the line (no rails) are numbered through it, and the
     * Production Terminal names them, the CPU and the warehouse's own Packing Station by their labels.
     */
    @ServusGameTest(timeoutTicks = 300)
    public static void lineOnlyMachinesNumberedAndShownByLabel(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(5, 2, 3);
        BlockPos lonePacking = new BlockPos(3, 2, 2);
        BlockPos terminal = new BlockPos(5, 3, 2);
        CoalGeneratorGameTests.place(helper, generatorPos, Direction.NORTH);
        helper.setBlock(lonePacking, ServusShuttlesBlocks.PACKING_STATION.get().defaultBlockState());
        CpuLine.setup(helper, true);
        helper.setBlock(terminal, ServusShuttlesBlocks.PRODUCTION_TERMINAL.get().defaultBlockState());
        CoalGeneratorBlockEntity generator = helper.getBlockEntity(generatorPos, CoalGeneratorBlockEntity.class);
        PackingStationBlockEntity lone = helper.getBlockEntity(lonePacking, PackingStationBlockEntity.class);
        PackingStationBlockEntity access = helper.getBlockEntity(CpuLine.ACCESS, PackingStationBlockEntity.class);
        whenNumbered(helper, () -> generator, () -> whenNumbered(helper, () -> lone, () -> {
            assertNumber(helper, generator, 1);
            helper.assertTrue(access.number() == 1 && lone.number() == 2, "the rail station is Packing #1 and the line-only one #2, got #"
                    + access.number() + " and #" + lone.number());
            generator.rename("Boiler");
            ProductionPayload view = ProductionView.of(helper.getLevel(), helper.absolutePos(terminal));
            MachineRow row = view.machines().stream().filter(machine -> machine.pos().equals(generator.getBlockPos())).findFirst().orElseThrow();
            helper.assertTrue(row.name().getString().equals("Boiler (Coal Generator #1)"), "the generator row reads its label, got " + row.name().getString());
            helper.assertTrue(view.packing().stream().map(packing -> packing.label().getString()).toList().containsAll(List.of("Packing #1", "Packing #2")),
                    "both Packing Stations read their labels, got " + view.packing());
            helper.assertTrue(view.cpus().get(0).label().getString().equals("CPU #1"), "the CPU row reads CPU #1, got " + view.cpus().get(0).label());
            helper.succeed();
        }));
    }

    /** A Crafting Station on a data line that reaches no warehouse stays unnumbered and reads as its block. */
    @ServusGameTest(timeoutTicks = 200)
    public static void lineWithoutWarehouseGivesNoNumber(GameTestHelper helper) {
        CraftingStationBlockEntity station = StationJobs.craftingStation(helper, CpuLine.STATION);
        TerminalWiring.dataDuct(helper, CpuLine.STATION.above());
        helper.runAfterDelay(100, () -> {
            assertNumber(helper, station, 0);
            labelArgs(helper, station.label(), "block.servus_shuttles.crafting_station");
            helper.succeed();
        });
    }

    /** "Sticks" (#1) dismantled with the carrot and placed on the same line keeps its name and number. */
    @ServusGameTest(timeoutTicks = 300)
    public static void dismantledCraftingStationKeepsItsLabel(GameTestHelper helper) {
        CpuLine.setup(helper, false);
        CraftingStationBlockEntity station = helper.getBlockEntity(CpuLine.STATION, CraftingStationBlockEntity.class);
        whenNumbered(helper, () -> station, () -> {
            station.rename("Sticks");
            BlockState state = helper.getBlockState(CpuLine.STATION);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            GameTestSteps.useCarrot(helper, player, CpuLine.STATION, Direction.UP);
            ItemStack item = GameTestSteps.drops(helper, CpuLine.STATION, 1).stream().map(ItemEntity::getItem)
                    .filter(stack -> stack.is(ServusShuttlesItems.CRAFTING_STATION.get())).findFirst().orElseThrow().copy();
            GameTestSteps.drops(helper, CpuLine.STATION, 2).forEach(ItemEntity::discard);
            helper.setBlock(SECOND_STATION, state);
            CraftingStationBlockEntity moved = helper.getBlockEntity(SECOND_STATION, CraftingStationBlockEntity.class);
            BlockItem.updateCustomBlockEntityTag(helper.getLevel(), player, helper.absolutePos(SECOND_STATION), item);
            moved.applyComponentsFromItemStack(item);
            helper.runAfterDelay(60, () -> {
                assertNumber(helper, moved, 1);
                helper.assertTrue(moved.name().equals("Sticks"), "the name travels with the item, got '" + moved.name() + "'");
                helper.succeed();
            });
        });
    }
}
