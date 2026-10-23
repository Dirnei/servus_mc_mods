package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleFleet;
import io.github.dirnei.mc.servus.shuttles.shuttle.StrandReason;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Cargo that does not fit where it was going is routed again or parked in the garage, never dropped or sent back. */
public final class LeftoverGameTests {

    private static final BlockPos ORIGIN = new BlockPos(3, 1, 4);
    private static final int CHEST_STACKS = 10;

    private LeftoverGameTests() {
    }

    private static int dropped(GameTestHelper helper, Item item) {
        int count = 0;
        for (ItemEntity entity : helper.getEntities(EntityType.ITEM)) {
            if (entity.getItem().is(item)) {
                count += entity.getItem().getCount();
            }
        }
        return count;
    }

    private static ChestBlockEntity chestOfCobblestone(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(pos, ChestBlockEntity.class);
        for (int slot = 0; slot < CHEST_STACKS; slot++) {
            chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        return chest;
    }

    /** A rail west of the mini warehouse, further from its station, with a rack north of it facing the rail. */
    private static HeavyRackBlockEntity farRack(GameTestHelper helper, MiniWarehouse warehouse) {
        BlockPos rail = warehouse.railPos(0).west();
        WarehouseFixtures.railOnStone(helper, rail);
        return WarehouseFixtures.rack(helper, rail.north(), Direction.SOUTH);
    }

    /**
     * The reported spill: a nearly full rack and an Inbound Station pulling
     * fast from two chests on its sides. Every cobblestone ends in the rack, the
     * buffer or a chest, and nothing drops.
     */
    @ServusGameTest(timeoutTicks = 1200)
    public static void fastPullsIntoAFullRackDropNothing(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        WarehouseFixtures.fill(warehouse.rack(), Items.STONE);
        warehouse.rack().items().setStackInSlot(0, ItemStack.EMPTY);
        warehouse.rack().items().setStackInSlot(1, new ItemStack(Items.COBBLESTONE, 40));
        InboundStationBlockEntity station = warehouse.withInbound();
        BlockPos stationPos = warehouse.inboundPos();
        ChestBlockEntity east = chestOfCobblestone(helper, stationPos.east());
        ChestBlockEntity west = chestOfCobblestone(helper, stationPos.west());
        station.sideConfig().set(station.relativeSide(Direction.EAST), SideMode.PULL);
        station.sideConfig().set(station.relativeSide(Direction.WEST), SideMode.PULL);
        int total = 40 + 2 * CHEST_STACKS * 64;
        Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 128 && warehouse.network().ridingJobs() == 0
                        && warehouse.network().queuedJobs() == 0 && WarehouseFixtures.blocked(helper, stationPos), 1150,
                () -> "the rack should fill up and the station wait, rack has " + warehouse.rackCount(Items.COBBLESTONE), () -> {
                    int kept = warehouse.rackCount(Items.COBBLESTONE) + WarehouseFixtures.count(station.buffer(), Items.COBBLESTONE)
                            + east.countItem(Items.COBBLESTONE) + west.countItem(Items.COBBLESTONE);
                    helper.assertTrue(dropped(helper, Items.COBBLESTONE) == 0, "no cobblestone drops");
                    helper.assertTrue(kept == total, "every cobblestone is in the rack, the buffer or a chest: " + kept + " of " + total);
                    helper.succeed();
                });
    }

    /** Cargo whose rack is broken on the way is routed to another rack. */
    @ServusGameTest(timeoutTicks = 600)
    public static void leftoverIsRoutedAgain(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        HeavyRackBlockEntity far = farRack(helper, warehouse);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 40), false);
            Waits.when(helper, () -> station.isEmpty() && warehouse.network().ridingJobs() == 1, MiniWarehouse.TRIP_TICKS * 2,
                    () -> "the shuttle should load the cobblestone", () -> {
                        helper.assertTrue(!warehouse.rack().items().locked().isEmpty() && far.items().locked().isEmpty(),
                                "the near rack holds the room for the cobblestone");
                        helper.setBlock(warehouse.rackPos(), Blocks.AIR.defaultBlockState());
                        Waits.when(helper, () -> WarehouseFixtures.count(far, Items.COBBLESTONE) == 40, MiniWarehouse.TRIP_TICKS * 4,
                                () -> "the cobblestone should end in the far rack, it has " + WarehouseFixtures.count(far, Items.COBBLESTONE), () -> {
                                    helper.assertTrue(dropped(helper, Items.COBBLESTONE) == 0, "nothing drops");
                                    helper.succeed();
                                });
                    });
        });
    }

    /**
     * With no rack left the shuttle parks the cargo in its garage: the lamp
     * turns red and the garage names it, a reload keeps it, and a new rack
     * gets the cargo.
     */
    @ServusGameTest(timeoutTicks = 900)
    public static void noRoomParksInTheGarage(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 40), false);
            Waits.when(helper, () -> station.isEmpty() && warehouse.network().ridingJobs() == 1, MiniWarehouse.TRIP_TICKS * 2,
                    () -> "the shuttle should load the cobblestone", () -> {
                        helper.setBlock(warehouse.rackPos(), Blocks.AIR.defaultBlockState());
                        BlockPos garage = helper.absolutePos(warehouse.garagePos());
                        Waits.when(helper, () -> WarehouseFixtures.blocked(helper, warehouse.garagePos())
                                        && WarehouseFixtures.lit(helper, warehouse.garagePos()) && warehouse.network().shuttleViews(garage).isEmpty(),
                                MiniWarehouse.TRIP_TICKS * 3,
                                () -> "the garage lamp should turn red", () -> {
                                    List<ShuttleFleet.Stranded> stranded = warehouse.network().stranded(garage);
                                    helper.assertTrue(stranded.size() == 1 && stranded.getFirst().cargo().getCount() == 40
                                            && stranded.getFirst().reason() == StrandReason.NO_ROOM, "the garage names the parked cargo: " + stranded);
                                    PersistGameTests.reload(helper, warehouse.garagePos());
                                    helper.runAfterDelay(30, () -> {
                                        helper.assertTrue(warehouse.network().stranded(garage).size() == 1, "the cargo is still parked after a reload");
                                        helper.assertTrue(dropped(helper, Items.COBBLESTONE) == 0, "nothing drops");
                                        WarehouseFixtures.rack(helper, warehouse.rackPos(), Direction.SOUTH);
                                        Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 40, MiniWarehouse.TRIP_TICKS * 4,
                                                () -> "the parked cargo should go into the new rack", () -> {
                                                    helper.assertTrue(WarehouseFixtures.activity(helper, warehouse.garagePos())
                                                            != StorageNodeBlocks.Activity.BLOCKED, "the lamp is no longer red");
                                                    helper.succeed();
                                                });
                                    });
                                });
                    });
        });
    }

    /** A pick whose destination is broken on the way stores its cargo in a rack and drops nothing. */
    @ServusGameTest(timeoutTicks = 600)
    public static void pickForABrokenDestinationIsStored(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).stock(new ItemStack(Items.IRON_INGOT, 64));
        OutboundStationBlockEntity station = WarehouseFixtures.outbound(helper, warehouse.railPos(0).south(), Direction.SOUTH);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(warehouse.network().feedStation(station, ItemKey.of(new ItemStack(Items.IRON_INGOT)), 16).isEmpty(),
                    "the feed is planned");
            Waits.when(helper, () -> warehouse.rackCount(Items.IRON_INGOT) == 48, MiniWarehouse.TRIP_TICKS * 2,
                    () -> "the shuttle should load the iron", () -> {
                        helper.setBlock(warehouse.railPos(0).south(), Blocks.AIR.defaultBlockState());
                        Waits.when(helper, () -> warehouse.rackCount(Items.IRON_INGOT) == 64 && warehouse.network().ridingJobs() == 0,
                                MiniWarehouse.TRIP_TICKS * 3, () -> "the iron should go back into a rack, rack has "
                                        + warehouse.rackCount(Items.IRON_INGOT), () -> {
                                    helper.assertTrue(dropped(helper, Items.IRON_INGOT) == 0, "no iron drops");
                                    helper.succeed();
                                });
                    });
        });
    }
}
