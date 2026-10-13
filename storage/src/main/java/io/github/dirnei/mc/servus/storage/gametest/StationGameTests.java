package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineTestSupport;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Inbound and Outbound Stations: docking, buffers, side modes and feeding. */
public final class StationGameTests {

    private static final BlockPos ST_CONTROLLER = new BlockPos(9, 1, 3);
    private static final BlockPos ST_STATION = new BlockPos(10, 1, 3);
    private static final BlockPos ST_CHEST = new BlockPos(11, 1, 3);
    private static final BlockPos DK_STATION = new BlockPos(5, 1, 5);
    private static final BlockPos OS_ORIGIN = new BlockPos(2, 1, 4);
    private static final BlockPos OS_STATION = new BlockPos(3, 1, 5);
    private static final BlockPos OS_FRONT = new BlockPos(4, 1, 5);
    private static final BlockPos FD_CONTROLLER = new BlockPos(1, 1, 7);
    private static final BlockPos FD_RACK = new BlockPos(4, 1, 8);
    private static final BlockPos FD_GARAGE = new BlockPos(2, 1, 8);
    private static final BlockPos FD_STATION = new BlockPos(7, 1, 8);
    private static final BlockPos IS_STATION = new BlockPos(5, 1, 10);
    private static final BlockPos IP_STATION = new BlockPos(8, 1, 10);

    private StationGameTests() {
    }

    private static InboundStationBlockEntity stationSetup(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, ST_CONTROLLER);
        WarehouseFixtures.inbound(helper, ST_STATION, Direction.NORTH);
        TerminalWiring.railUnder(helper, ST_STATION);
        return helper.getBlockEntity(ST_STATION, InboundStationBlockEntity.class);
    }

    /** Neither a chest feeding the station nor the buffer itself counts as warehouse stock. */
    @ServusGameTest(timeoutTicks = 200)
    public static void chestOnStationIsNotStock(GameTestHelper helper) {
        InboundStationBlockEntity station = stationSetup(helper);
        helper.setBlock(ST_CHEST, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(ST_CHEST, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.BREAD, 30));
        helper.runAfterDelay(5, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.BREAD, 10), false);
            helper.runAfterDelay(15, () -> {
                WarehouseControllerBlockEntity controller = helper.getBlockEntity(ST_CONTROLLER, WarehouseControllerBlockEntity.class);
                WarehouseNetwork network = controller.network().orElseThrow();
                helper.assertTrue(network.index().count(ItemKey.of(new ItemStack(Items.BREAD))) == 0,
                        "bread beside or inside the station must not be indexed");
                helper.assertTrue(!station.isEmpty(), "with no storage the bread waits in the buffer");
                helper.assertTrue(station.blockedReason() == PutAwayPlan.Reason.NO_STORAGE, "reason should be no storage");
                helper.succeed();
            });
        });
    }

    /** Rails dock onto a station on every side, and the carrot dismantles it with its buffer. */
    @ServusGameTest(timeoutTicks = 100)
    public static void stationDocksAndDismantles(GameTestHelper helper) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(DK_STATION.relative(direction).below(), Blocks.STONE.defaultBlockState());
        }
        WarehouseFixtures.inbound(helper, DK_STATION, Direction.NORTH);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            WarehouseFixtures.rail(helper, DK_STATION.relative(direction));
        }
        InboundStationBlockEntity station = helper.getBlockEntity(DK_STATION, InboundStationBlockEntity.class);
        Transfer.insertItem(station.intake(), new ItemStack(Items.BREAD, 10), false);
        helper.runAfterDelay(2, () -> {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockState rail = helper.getBlockState(DK_STATION.relative(direction));
                helper.assertTrue(rail.getValue(RailBlock.dock(direction.getOpposite())),
                        "rail " + direction + " of the station should dock towards it");
            }
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            MachineTestSupport.useCarrot(helper, player, DK_STATION, Direction.UP);
            helper.runAfterDelay(2, () -> {
                helper.assertBlockPresent(Blocks.AIR, DK_STATION);
                helper.assertItemEntityPresent(ServusStorageItems.INBOUND_STATION.get(), DK_STATION, 2.0);
                helper.assertItemEntityPresent(Items.BREAD, DK_STATION, 2.0);
                helper.succeed();
            });
        });
    }

    /** The station menu reports the warehouse state and shift click feeds the buffer. */
    @ServusGameTest(timeoutTicks = 200)
    public static void stationMenuFeedsBuffer(GameTestHelper helper) {
        InboundStationBlockEntity station = stationSetup(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(5, () -> {
            InboundStationMenu menu = new InboundStationMenu(1, player.getInventory(), station);
            helper.assertTrue(menu.state().online(), "the station should see an online warehouse");
            helper.assertTrue(menu.state().idle(), "an empty buffer reads idle");

            player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 20));
            menu.quickMoveStack(player, menu.inventoryStart() + 27);
            helper.assertTrue(station.buffer().getStackInSlot(0).getCount() == 20 && station.buffer().getStackInSlot(0).is(Items.COBBLESTONE),
                    "shift click should move the cobblestone into the buffer");
            helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "the inventory slot should be empty");
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!menu.state().blockedReason().isEmpty() && menu.state().blockedStack().is(Items.COBBLESTONE),
                        "cobblestone has no storage and the state should say so: " + menu.state());
                helper.succeed();
            });
        });
    }

    /** A mini warehouse with an Outbound Station south of its middle rail, facing west: its back, and so its machine, is east of it. */
    private static OutboundStationBlockEntity outboundSetup(GameTestHelper helper) {
        MiniWarehouse.build(helper, OS_ORIGIN);
        return WarehouseFixtures.outbound(helper, OS_STATION, Direction.WEST);
    }

    private static HeavyRackBlockEntity outboundRack(GameTestHelper helper) {
        return helper.getBlockEntity(OS_ORIGIN.north(), HeavyRackBlockEntity.class);
    }

    /** A keep slot pulls its amount out of storage into the buffer. */
    @ServusGameTest(timeoutTicks = 200)
    public static void outboundKeepsBufferStocked(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        HeavyRackBlockEntity rack = outboundRack(helper);
        WarehouseFixtures.stock(helper, OS_ORIGIN.north(), new ItemStack(Items.STICK, 50));
        station.setKeep(0, new ItemStack(Items.STICK), 16);
        Waits.when(helper, () -> station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))) == 16,
                StorageStatsManager.INSTANCE.outboundCheckInterval() + MiniWarehouse.TRIP_TICKS,
                () -> "16 sticks should be in the buffer, had " + station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))), () -> {
            helper.assertTrue(WarehouseFixtures.count(rack, Items.STICK) == 34, "34 sticks should remain in the rack, had " + WarehouseFixtures.count(rack, Items.STICK));
            helper.assertTrue(station.shortStack().isEmpty(), "nothing should be short");
            helper.succeed();
        });
    }

    /** What a hopper takes from an extract side is topped up again. */
    @ServusGameTest(timeoutTicks = 600)
    public static void outboundToppedUpAfterExtraction(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        HeavyRackBlockEntity rack = outboundRack(helper);
        WarehouseFixtures.stock(helper, OS_ORIGIN.north(), new ItemStack(Items.STICK, 50));
        station.setKeep(0, new ItemStack(Items.STICK), 16);
        int interval = StorageStatsManager.INSTANCE.outboundCheckInterval();
        Waits.when(helper, () -> station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))) == 16, interval + MiniWarehouse.TRIP_TICKS,
                () -> "16 sticks should be in the buffer first", () -> {
            helper.assertTrue(station.handlerFor(Direction.DOWN) == null, "the bottom is off by default");
            helper.assertTrue(station.cycleFace(Direction.DOWN) == SideMode.PUSH, "the first cycle turns the bottom to push");
            helper.assertTrue(station.cycleFace(Direction.DOWN) == SideMode.OUTPUT, "the second cycle turns the bottom to output");
            ItemView bottom = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(OS_STATION), Direction.DOWN));
            helper.assertTrue(bottom != null, "the extract side exposes a handler");
            helper.assertTrue(bottom.extractItem(0, 5, false).getCount() == 5, "5 sticks can be taken");
            helper.assertTrue(bottom.insertItem(1, new ItemStack(Items.COBBLESTONE, 3), false).getCount() == 3, "nothing goes in");
            Waits.when(helper, () -> station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))) == 16, interval + MiniWarehouse.TRIP_TICKS * 2,
                    () -> "the buffer should be topped up to 16, had " + station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))), () -> {
                helper.assertTrue(WarehouseFixtures.count(rack, Items.STICK) == 29, "29 sticks should remain in the rack, had " + WarehouseFixtures.count(rack, Items.STICK));
                helper.succeed();
            });
        });
    }

    /** The buffer is pushed into the block in front. */
    @ServusGameTest(timeoutTicks = 100)
    public static void outboundPushesIntoFront(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        helper.setBlock(OS_FRONT, Blocks.CHEST.defaultBlockState());
        station.buffer().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
        helper.runAfterDelay(20, () -> {
            ChestBlockEntity chest = helper.getBlockEntity(OS_FRONT, ChestBlockEntity.class);
            helper.assertTrue(chest.countItem(Items.IRON_INGOT) == 8, "the chest should hold the 8 ingots, had " + chest.countItem(Items.IRON_INGOT));
            helper.assertTrue(station.isEmpty(), "the buffer should be empty");
            helper.succeed();
        });
    }

    /** Sides cycle push, output, off; off sides expose nothing, the back starts as push and the front is fixed off. */
    @ServusGameTest(timeoutTicks = 100)
    public static void outboundSideModes(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        helper.assertTrue(station.sideMode(Direction.EAST) == SideMode.PUSH, "the back starts as push");
        helper.assertTrue(station.relativeSide(Direction.EAST) == RelativeSide.BACK, "east is the back of a station facing west");
        helper.assertTrue(station.handlerFor(Direction.EAST) != null, "a push side also lets neighbours extract");
        helper.assertTrue(station.sideMode(Direction.WEST) == SideMode.DISABLED && station.cycleFace(Direction.WEST) == SideMode.DISABLED, "the front is fixed off");
        helper.assertTrue(station.sideMode(Direction.UP) == SideMode.DISABLED && station.handlerFor(Direction.UP) == null, "other sides start off");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.PUSH, "off cycles to push");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.OUTPUT, "push cycles to output");
        helper.assertTrue(station.handlerFor(Direction.UP) != null, "an output side exposes the buffer");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.DISABLED, "output cycles to off");
        helper.assertTrue(station.handlerFor(Direction.UP) == null, "an off side exposes nothing");
        station.sideConfig().set(RelativeSide.TOP, SideMode.BOTH);
        helper.assertTrue(station.sideMode(Direction.UP) == SideMode.DISABLED, "an unsupported mode falls back to off");
        helper.succeed();
    }

    /** Feeding a station from a rack rides a shuttle. */
    @ServusGameTest(timeoutTicks = 400)
    public static void feedOutboundByShuttle(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, FD_CONTROLLER);
        WarehouseFixtures.railRow(helper, 2, 7, 7);
        WarehouseFixtures.rack(helper, FD_RACK, Direction.NORTH);
        WarehouseFixtures.garage(helper, FD_GARAGE, Direction.SOUTH, 0);
        WarehouseFixtures.outbound(helper, FD_STATION, Direction.SOUTH);
        HeavyRackBlockEntity rack = helper.getBlockEntity(FD_RACK, HeavyRackBlockEntity.class);
        rack.items().insertItem(0, new ItemStack(Items.IRON_INGOT, 10), false);
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(FD_GARAGE, ShuttleGarageBlockEntity.class);
        garage.addShuttle();
        OutboundStationBlockEntity station = helper.getBlockEntity(FD_STATION, OutboundStationBlockEntity.class);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(FD_CONTROLLER, WarehouseControllerBlockEntity.class);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            String refused = network.feedStation(station, ItemKey.of(new ItemStack(Items.IRON_INGOT)), 4);
            helper.assertTrue(refused.isEmpty(), "the feed should go through, got " + refused);
            helper.assertTrue(station.isEmpty(), "nothing arrives at once from a rack");
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(station.bufferCount(ItemKey.of(new ItemStack(Items.IRON_INGOT))) == 4, "the shuttle should have brought 4 ingots, buffer has " + station.bufferCount(ItemKey.of(new ItemStack(Items.IRON_INGOT))));
                helper.succeed();
            });
        });
    }

    /** An inbound side switched off takes nothing until it is switched on again. */
    @ServusGameTest(timeoutTicks = 200)
    public static void inboundSideOff(GameTestHelper helper) {
        InboundStationBlockEntity station = WarehouseFixtures.inbound(helper, IS_STATION, Direction.NORTH);
        helper.assertTrue(station.sideMode(Direction.NORTH) == SideMode.DISABLED && station.cycleFace(Direction.NORTH) == SideMode.DISABLED, "the front is fixed off");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.PULL, "input cycles to pull");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.DISABLED, "pull cycles to off");
        helper.setBlock(IS_STATION.above(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(IS_STATION.above(), HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.BREAD, 10));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(station.isEmpty() && hopper.getItem(0).getCount() == 10, "the hopper should not get through an off side");
            helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.INPUT, "off cycles to input");
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(!station.isEmpty(), "the hopper should feed the accepting side");
                helper.succeed();
            });
        });
    }

    /** A Pull side empties a chest into the buffer without a hopper. */
    @ServusGameTest(timeoutTicks = 200)
    public static void inboundPullsFromChest(GameTestHelper helper) {
        InboundStationBlockEntity station = WarehouseFixtures.inbound(helper, IP_STATION, Direction.NORTH);
        helper.setBlock(IP_STATION.south(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(IP_STATION.south(), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.BREAD, 30));
        helper.assertTrue(station.relativeSide(Direction.SOUTH) == RelativeSide.BACK, "south is the back of a station facing north");
        station.sideConfig().set(RelativeSide.BACK, SideMode.PULL);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(chest.countItem(Items.BREAD) == 0, "the chest should be emptied, has " + chest.countItem(Items.BREAD));
            helper.assertTrue(station.buffer().getStackInSlot(0).getCount() == 30, "the bread should be in the buffer");
            helper.succeed();
        });
    }
}
