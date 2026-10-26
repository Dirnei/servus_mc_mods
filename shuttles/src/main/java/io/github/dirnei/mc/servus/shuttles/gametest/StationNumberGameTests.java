package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.menu.PackingStationMenu;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.station.StationBlock;
import io.github.dirnei.mc.servus.shuttles.station.StationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.station.StationCounters;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Station numbers per warehouse, their survival through dismantles and saves, and station names. */
public final class StationNumberGameTests {

    /** The rail row of the first warehouse runs along z 2, its stations stand at z 3. */
    private static final int ROW_A = 2;
    /** The rail row of a second warehouse runs along z 8, its stations stand at z 9. */
    private static final int ROW_B = 8;
    private static final int WAIT = 60;

    private StationNumberGameTests() {
    }

    /** Stone and rails from x 1 to 10 along the row, and the controller at x 0 of the row. */
    private static WarehouseControllerBlockEntity warehouse(GameTestHelper helper, int row) {
        rails(helper, row);
        return WarehouseFixtures.controller(helper, new BlockPos(0, 1, row));
    }

    private static void rails(GameTestHelper helper, int row) {
        WarehouseFixtures.railRow(helper, 1, 10, row);
    }

    private static BlockPos spot(int x, int row) {
        return new BlockPos(x, 1, row + 1);
    }

    private static OutboundStationBlockEntity outbound(GameTestHelper helper, int x, int row) {
        return WarehouseFixtures.outbound(helper, spot(x, row), Direction.NORTH);
    }

    private static InboundStationBlockEntity inbound(GameTestHelper helper, int x, int row) {
        return WarehouseFixtures.inbound(helper, spot(x, row), Direction.NORTH);
    }

    private static void whenNumbered(GameTestHelper helper, Supplier<? extends StationBlockEntity> station, Runnable then) {
        Waits.when(helper, () -> station.get().number() > 0, WAIT, () -> "the station should get a number", then);
    }

    private static void assertNumber(GameTestHelper helper, StationBlockEntity station, int number) {
        helper.assertTrue(station.number() == number, "expected #" + number + ", got #" + station.number());
    }

    /** Dismantles the station at the spot with a sneaking carrot click and picks up its item. */
    private static ItemStack dismantle(GameTestHelper helper, BlockPos pos, Item item) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        GameTestSteps.useCarrot(helper, player, pos, Direction.UP);
        helper.assertBlockPresent(Blocks.AIR, pos);
        ItemStack drop = GameTestSteps.drops(helper, pos, 1).stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(item)).findFirst().orElseThrow();
        GameTestSteps.drops(helper, pos, 1).forEach(ItemEntity::discard);
        return drop.copy();
    }

    /** Places a block item as a player would: the block, the item's block entity data, then its components. */
    private static <T extends StationBlockEntity> T place(GameTestHelper helper, BlockPos pos, BlockState state, ItemStack item, Class<T> type) {
        helper.setBlock(pos, state);
        T station = helper.getBlockEntity(pos, type);
        BlockItem.updateCustomBlockEntityTag(helper.getLevel(), helper.makeMockPlayer(GameType.SURVIVAL), helper.absolutePos(pos), item);
        station.applyComponentsFromItemStack(item);
        return station;
    }

    private static Object[] labelArgs(GameTestHelper helper, Component label, String key) {
        helper.assertTrue(label.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                "expected the label " + key + ", got " + label);
        return ((TranslatableContents) label.getContents()).getArgs();
    }

    /** Two Outbound Stations and one Inbound Station linked one after the other are Outbound #1, #2 and Inbound #1. */
    @ServusGameTest(timeoutTicks = 200)
    public static void firstStationsNumberedPerKind(GameTestHelper helper) {
        warehouse(helper, ROW_A);
        OutboundStationBlockEntity first = outbound(helper, 6, ROW_A);
        whenNumbered(helper, () -> first, () -> {
            OutboundStationBlockEntity second = outbound(helper, 2, ROW_A);
            whenNumbered(helper, () -> second, () -> {
                InboundStationBlockEntity intake = inbound(helper, 4, ROW_A);
                whenNumbered(helper, () -> intake, () -> {
                    assertNumber(helper, first, 1);
                    assertNumber(helper, second, 2);
                    assertNumber(helper, intake, 1);
                    Object[] args = labelArgs(helper, second.label(), "station.servus_shuttles.label.outbound");
                    helper.assertTrue(args[0].equals(2), "the label reads Outbound #2");
                    labelArgs(helper, intake.getDisplayName(), "station.servus_shuttles.label.inbound");
                    helper.succeed();
                });
            });
        });
    }

    /** A broken Outbound #2 leaves its number unused: the next Outbound Station is #3. */
    @ServusGameTest(timeoutTicks = 200)
    public static void brokenNumberNotReused(GameTestHelper helper) {
        warehouse(helper, ROW_A);
        OutboundStationBlockEntity first = outbound(helper, 2, ROW_A);
        OutboundStationBlockEntity second = outbound(helper, 4, ROW_A);
        whenNumbered(helper, () -> second, () -> {
            assertNumber(helper, second, 2);
            helper.setBlock(spot(4, ROW_A), Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(3, () -> {
                OutboundStationBlockEntity third = outbound(helper, 6, ROW_A);
                whenNumbered(helper, () -> third, () -> {
                    assertNumber(helper, first, 1);
                    assertNumber(helper, third, 3);
                    helper.succeed();
                });
            });
        });
    }

    /** Numbers, the warehouse identity and its counters survive saving and loading the block entities. */
    @ServusGameTest(timeoutTicks = 200)
    public static void numbersSurviveSaving(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper, ROW_A);
        InboundStationBlockEntity first = inbound(helper, 2, ROW_A);
        InboundStationBlockEntity second = inbound(helper, 4, ROW_A);
        whenNumbered(helper, () -> second, () -> {
            first.rename("Quarry Intake");
            CompoundTag stationTag = first.saveWithFullMetadata(helper.getLevel().registryAccess());
            InboundStationBlockEntity station = new InboundStationBlockEntity(first.getBlockPos(), first.getBlockState());
            station.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), stationTag));
            helper.assertTrue(station.number() == 1 && station.name().equals("Quarry Intake") && !station.needsNumber(controller.warehouseId()),
                    "the loaded station is still Quarry Intake (#1) of this warehouse, got #" + station.number() + " " + station.name());

            CompoundTag controllerTag = controller.saveWithFullMetadata(helper.getLevel().registryAccess());
            WarehouseControllerBlockEntity loaded = new WarehouseControllerBlockEntity(controller.getBlockPos(), controller.getBlockState());
            loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), controllerTag));
            helper.assertTrue(loaded.warehouseId().equals(controller.warehouseId()), "the warehouse keeps its identity");
            helper.assertTrue(loaded.lastStationNumber(StationCounters.Kind.INBOUND) == 2
                    && loaded.takeStationNumber(StationCounters.Kind.INBOUND) == 3, "the next Inbound Station is #3");
            helper.succeed();
        });
    }

    /** A station on rails without a controller gets no number; the controller placed later gives it #1. */
    @ServusGameTest(timeoutTicks = 200)
    public static void noNumberWhileOffline(GameTestHelper helper) {
        rails(helper, ROW_A);
        OutboundStationBlockEntity station = outbound(helper, 3, ROW_A);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(station.number() == 0, "an offline station has no number, got #" + station.number());
            labelArgs(helper, station.label(), "block.servus_shuttles.outbound_station");
            WarehouseFixtures.controller(helper, new BlockPos(0, 1, ROW_A));
            whenNumbered(helper, () -> station, () -> {
                assertNumber(helper, station, 1);
                helper.succeed();
            });
        });
    }

    /** Stations numbered at the same moment, as after a world loads, are numbered by their position. */
    @ServusGameTest(timeoutTicks = 200)
    public static void stationsNumberedTogetherByPosition(GameTestHelper helper) {
        rails(helper, ROW_A);
        OutboundStationBlockEntity east = outbound(helper, 8, ROW_A);
        OutboundStationBlockEntity west = outbound(helper, 2, ROW_A);
        OutboundStationBlockEntity middle = outbound(helper, 5, ROW_A);
        helper.runAfterDelay(5, () -> {
            WarehouseFixtures.controller(helper, new BlockPos(0, 1, ROW_A));
            whenNumbered(helper, () -> east, () -> {
                assertNumber(helper, west, 1);
                assertNumber(helper, middle, 2);
                assertNumber(helper, east, 3);
                helper.succeed();
            });
        });
    }

    /** Outbound #2 dismantled with the carrot and placed at another rail of the same warehouse stays #2; the next new one is #3. */
    @ServusGameTest(timeoutTicks = 200)
    public static void dismantledStationKeepsNumber(GameTestHelper helper) {
        warehouse(helper, ROW_A);
        outbound(helper, 2, ROW_A);
        OutboundStationBlockEntity second = outbound(helper, 4, ROW_A);
        whenNumbered(helper, () -> second, () -> {
            BlockState state = helper.getBlockState(spot(4, ROW_A));
            ItemStack item = dismantle(helper, spot(4, ROW_A), ServusShuttlesItems.OUTBOUND_STATION.get());
            OutboundStationBlockEntity moved = place(helper, spot(8, ROW_A), state, item, OutboundStationBlockEntity.class);
            helper.assertTrue(moved.number() == 2, "the placed station is still #2, got #" + moved.number());
            helper.runAfterDelay(5, () -> {
                assertNumber(helper, moved, 2);
                OutboundStationBlockEntity fresh = outbound(helper, 6, ROW_A);
                whenNumbered(helper, () -> fresh, () -> {
                    assertNumber(helper, fresh, 3);
                    helper.succeed();
                });
            });
        });
    }

    /** Outbound #2 named "Compost Dock" placed into a second warehouse whose last Outbound Station is #1 reads "Compost Dock (#2)" there. */
    @ServusGameTest(timeoutTicks = 200)
    public static void stationInAnotherWarehouseGetsItsNumber(GameTestHelper helper) {
        warehouse(helper, ROW_A);
        warehouse(helper, ROW_B);
        outbound(helper, 2, ROW_A);
        OutboundStationBlockEntity second = outbound(helper, 4, ROW_A);
        OutboundStationBlockEntity other = outbound(helper, 2, ROW_B);
        whenNumbered(helper, () -> second, () -> whenNumbered(helper, () -> other, () -> {
            second.rename("Compost Dock");
            BlockState state = helper.getBlockState(spot(4, ROW_A));
            ItemStack item = dismantle(helper, spot(4, ROW_A), ServusShuttlesItems.OUTBOUND_STATION.get());
            OutboundStationBlockEntity moved = place(helper, spot(6, ROW_B), state, item, OutboundStationBlockEntity.class);
            Waits.when(helper, () -> moved.number() == 2 && moved.name().equals("Compost Dock") && !moved.needsNumber(otherId(helper)), WAIT,
                    () -> "the station should be Compost Dock (#2) of the second warehouse, got #" + moved.number() + " " + moved.name(), () -> {
                        Object[] args = labelArgs(helper, moved.label(), "station.servus_shuttles.label.named");
                        helper.assertTrue(args[0].equals("Compost Dock") && args[1].equals(2), "the label reads Compost Dock (#2)");
                        helper.succeed();
                    });
        }));
    }

    private static UUID otherId(GameTestHelper helper) {
        return helper.getBlockEntity(new BlockPos(0, 1, ROW_B), WarehouseControllerBlockEntity.class).warehouseId();
    }

    /** The controller dismantled with the carrot keeps the count; broken without it the stations are numbered from 1 again. */
    @ServusGameTest(timeoutTicks = 300)
    public static void controllerCarriesTheCount(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper, ROW_A);
        InboundStationBlockEntity first = inbound(helper, 2, ROW_A);
        InboundStationBlockEntity second = inbound(helper, 4, ROW_A);
        whenNumbered(helper, () -> second, () -> {
            UUID id = controller.warehouseId();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            BlockPos at = new BlockPos(0, 1, ROW_A);
            GameTestSteps.useCarrot(helper, player, at, Direction.NORTH);
            ItemStack item = GameTestSteps.drops(helper, at, 1).stream().map(ItemEntity::getItem)
                    .filter(stack -> stack.is(ServusShuttlesItems.WAREHOUSE_CONTROLLER.get())).findFirst()
                    .orElseThrow().copy();
            GameTestSteps.drops(helper, at, 1).forEach(ItemEntity::discard);
            BlockPos again = new BlockPos(11, 1, ROW_A);
            WarehouseControllerBlockEntity placed = WarehouseFixtures.placeController(helper, again, player, item);
            helper.assertTrue(placed.warehouseId().equals(id), "the warehouse identity travels with the controller");
            InboundStationBlockEntity third = inbound(helper, 6, ROW_A);
            whenNumbered(helper, () -> third, () -> {
                assertNumber(helper, first, 1);
                assertNumber(helper, second, 2);
                assertNumber(helper, third, 3);
                helper.setBlock(again, Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(3, () -> {
                    WarehouseControllerBlockEntity fresh = WarehouseFixtures.controller(helper, at);
                    Waits.when(helper, () -> !first.needsNumber(fresh.warehouseId()) && !second.needsNumber(fresh.warehouseId())
                                    && !third.needsNumber(fresh.warehouseId()), WAIT,
                            () -> "the stations should be numbered by the new controller", () -> {
                                List<Integer> numbers = List.of(first.number(), second.number(), third.number());
                                helper.assertTrue(numbers.equals(List.of(1, 2, 3)), "numbered again from 1 by position, got " + numbers);
                                helper.succeed();
                            });
                });
            });
        });
    }

    /** The GUI renames a station and cleans the name; a cleared name removes it, and the label follows. */
    @ServusGameTest(timeoutTicks = 200)
    public static void menuRenamesStation(GameTestHelper helper) {
        warehouse(helper, ROW_A);
        OutboundStationBlockEntity station = outbound(helper, 3, ROW_A);
        whenNumbered(helper, () -> station, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(helper.absolutePos(spot(3, ROW_A))).add(1, 0, 0));
            OutboundStationMenu menu = new OutboundStationMenu(1, player.getInventory(), station);
            menu.rename("  Compost\nDock  ");
            helper.assertTrue(station.name().equals("CompostDock"), "the name is cleaned, got '" + station.name() + "'");
            Object[] args = labelArgs(helper, station.label(), "station.servus_shuttles.label.named");
            helper.assertTrue(args[0].equals("CompostDock") && args[1].equals(1), "the label reads CompostDock (#1)");
            menu.rename("");
            helper.assertTrue(station.name().isEmpty(), "a cleared field removes the name");
            labelArgs(helper, station.label(), "station.servus_shuttles.label.outbound");

            Player far = helper.makeMockPlayer(GameType.SURVIVAL);
            far.setPos(Vec3.atCenterOf(helper.absolutePos(spot(3, ROW_A))).add(40, 0, 0));
            new OutboundStationMenu(2, far.getInventory(), station).rename("Far Away");
            helper.assertTrue(station.name().isEmpty(), "a player who may not use the GUI cannot rename");
            helper.succeed();
        });
    }

    private static PackingStationBlockEntity packing(GameTestHelper helper, int x, int row) {
        helper.setBlock(spot(x, row), ServusShuttlesBlocks.PACKING_STATION.get().defaultBlockState().setValue(StationBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(spot(x, row), PackingStationBlockEntity.class);
    }

    /** An Inbound Station and a Packing Station of one warehouse read Inbound #1 and Packing #1; the next Inbound Station is #2. */
    @ServusGameTest(timeoutTicks = 200)
    public static void packingStationCountsOnItsOwn(GameTestHelper helper) {
        warehouse(helper, ROW_A);
        InboundStationBlockEntity intake = inbound(helper, 2, ROW_A);
        whenNumbered(helper, () -> intake, () -> {
            PackingStationBlockEntity packer = packing(helper, 4, ROW_A);
            whenNumbered(helper, () -> packer, () -> {
                InboundStationBlockEntity second = inbound(helper, 6, ROW_A);
                whenNumbered(helper, () -> second, () -> {
                    assertNumber(helper, intake, 1);
                    assertNumber(helper, packer, 1);
                    assertNumber(helper, second, 2);
                    labelArgs(helper, intake.label(), "station.servus_shuttles.label.inbound");
                    Object[] args = labelArgs(helper, packer.getDisplayName(), "station.servus_shuttles.label.packing");
                    helper.assertTrue(args[0].equals(1), "the label reads Packing #1");
                    helper.succeed();
                });
            });
        });
    }

    /** The Packing Station GUI renames the station; the label follows and a far player cannot rename. */
    @ServusGameTest(timeoutTicks = 200)
    public static void menuRenamesPackingStation(GameTestHelper helper) {
        warehouse(helper, ROW_A);
        inbound(helper, 2, ROW_A);
        PackingStationBlockEntity packer = packing(helper, 4, ROW_A);
        whenNumbered(helper, () -> packer, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(helper.absolutePos(spot(4, ROW_A))).add(1, 0, 0));
            new PackingStationMenu(1, player.getInventory(), packer).rename("Parcel Dock");
            Object[] args = labelArgs(helper, packer.label(), "station.servus_shuttles.label.named");
            helper.assertTrue(args[0].equals("Parcel Dock") && args[1].equals(1), "the label reads Parcel Dock (#1)");

            Player far = helper.makeMockPlayer(GameType.SURVIVAL);
            far.setPos(Vec3.atCenterOf(helper.absolutePos(spot(4, ROW_A))).add(40, 0, 0));
            new PackingStationMenu(2, far.getInventory(), packer).rename("Far Away");
            helper.assertTrue(packer.name().equals("Parcel Dock"), "a player who may not use the GUI cannot rename");
            helper.succeed();
        });
    }

    /** Packing #1 named "Parcel Dock" dismantled with the carrot and placed again in the same warehouse keeps its number and name. */
    @ServusGameTest(timeoutTicks = 200)
    public static void dismantledPackingStationKeepsNumber(GameTestHelper helper) {
        warehouse(helper, ROW_A);
        PackingStationBlockEntity packer = packing(helper, 4, ROW_A);
        whenNumbered(helper, () -> packer, () -> {
            packer.rename("Parcel Dock");
            BlockState state = helper.getBlockState(spot(4, ROW_A));
            ItemStack item = dismantle(helper, spot(4, ROW_A), ServusShuttlesItems.PACKING_STATION.get());
            PackingStationBlockEntity moved = place(helper, spot(8, ROW_A), state, item, PackingStationBlockEntity.class);
            helper.runAfterDelay(5, () -> {
                assertNumber(helper, moved, 1);
                helper.assertTrue(moved.name().equals("Parcel Dock"), "the name travels with the item, got '" + moved.name() + "'");
                helper.succeed();
            });
        });
    }

    /** A Packing Station saved with a number but no counter took it from the Inbound counter: it loads unnumbered, an Inbound Station keeps its number. */
    @ServusGameTest(timeoutTicks = 200)
    public static void packingStationDropsAnInboundNumber(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper, ROW_A);
        InboundStationBlockEntity intake = inbound(helper, 2, ROW_A);
        PackingStationBlockEntity packer = packing(helper, 4, ROW_A);
        whenNumbered(helper, () -> intake, () -> whenNumbered(helper, () -> packer, () -> {
            CompoundTag packerTag = packer.saveWithFullMetadata(helper.getLevel().registryAccess());
            packerTag.remove("StationCounter");
            PackingStationBlockEntity loadedPacker = new PackingStationBlockEntity(packer.getBlockPos(), packer.getBlockState());
            loadedPacker.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), packerTag));
            helper.assertTrue(loadedPacker.number() == 0 && loadedPacker.needsNumber(controller.warehouseId()),
                    "a Packing Station with an Inbound number is numbered again, got #" + loadedPacker.number());

            CompoundTag intakeTag = intake.saveWithFullMetadata(helper.getLevel().registryAccess());
            intakeTag.remove("StationCounter");
            InboundStationBlockEntity loadedIntake = new InboundStationBlockEntity(intake.getBlockPos(), intake.getBlockState());
            loadedIntake.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), intakeTag));
            helper.assertTrue(loadedIntake.number() == 1 && !loadedIntake.needsNumber(controller.warehouseId()),
                    "an Inbound Station keeps Inbound #1, got #" + loadedIntake.number());
            helper.succeed();
        }));
    }
}
