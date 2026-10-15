package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Shuttle served blocks join the warehouse only through rails. */
public final class StationRailGameTests {

    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos STATION = new BlockPos(3, 1, 2);
    /** The rack {@link MiniWarehouse#under} puts west of the rail under the controller. */
    private static final BlockPos RACK = new BlockPos(1, 0, 2);

    private StationRailGameTests() {
    }

    private static boolean sameWarehouse(GameTestHelper helper, WarehouseControllerBlockEntity controller, BlockPos pos) {
        StorageNodeBlockEntity node = helper.getBlockEntity(pos, StorageNodeBlockEntity.class);
        return node.network().isPresent() && node.network().equals(controller.network());
    }

    /** A station touching the controller stays out until a rail links it, and leaves again when the rail breaks. */
    @ServusGameTest(timeoutTicks = 400)
    public static void stationJoinsOnlyByRail(GameTestHelper helper) {
        MiniWarehouse.under(helper, CONTROLLER);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class);
        HeavyRackBlockEntity rack = helper.getBlockEntity(RACK, HeavyRackBlockEntity.class);
        InboundStationBlockEntity station = WarehouseFixtures.inbound(helper, STATION, Direction.NORTH);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!sameWarehouse(helper, controller, STATION), "touching the controller is not enough");
            helper.assertTrue(!WarehouseFixtures.lit(helper, STATION), "the lamp is dark without a rail");
            Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 64), false);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!station.isEmpty() && rack.isEmpty(), "the cobblestone waits in the buffer");
                WarehouseFixtures.rail(helper, STATION.below());
                Waits.when(helper, () -> WarehouseFixtures.count(rack, Items.COBBLESTONE) == 64 && station.isEmpty(), MiniWarehouse.TRIP_TICKS * 2,
                        () -> "with a rail a shuttle puts the cobblestone away, rack has " + WarehouseFixtures.count(rack, Items.COBBLESTONE), () -> {
                    helper.assertTrue(sameWarehouse(helper, controller, STATION) && WarehouseFixtures.lit(helper, STATION), "the rail links the station");
                    helper.setBlock(STATION.below(), Blocks.AIR.defaultBlockState());
                    Waits.when(helper, () -> !WarehouseFixtures.lit(helper, STATION), 20,
                            () -> "breaking the rail takes the station out", () -> {
                        helper.assertTrue(!sameWarehouse(helper, controller, STATION), "the station left the warehouse");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** A rack touching only a rail linked station does not join through it. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackBesideStationNotLinked(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, CONTROLLER);
        WarehouseFixtures.inbound(helper, STATION, Direction.NORTH);
        TerminalWiring.railUnder(helper, STATION);
        BlockPos beyond = STATION.east();
        WarehouseFixtures.rack(helper, beyond, Direction.WEST);
        WarehouseFixtures.stock(helper, beyond, new ItemStack(Items.BREAD, 12));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(sameWarehouse(helper, controller, STATION), "the station is linked by rail");
            long count = controller.network().orElseThrow().index().count(ItemKey.of(new ItemStack(Items.BREAD)));
            helper.assertTrue(count == 0, "bread beyond the station is not stock, counted " + count);
            helper.succeed();
        });
    }

    /** Packing Station, Outbound Station and Shuttle Garage pressed against the controller stay out without a rail. */
    @ServusGameTest(timeoutTicks = 100)
    public static void shuttleBlocksNeedARail(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, CONTROLLER);
        BlockPos access = CONTROLLER.east();
        BlockPos outbound = CONTROLLER.west();
        BlockPos garage = CONTROLLER.south();
        helper.setBlock(access, ServusShuttlesBlocks.PACKING_STATION.get().defaultBlockState());
        WarehouseFixtures.outbound(helper, outbound, Direction.WEST);
        WarehouseFixtures.garage(helper, garage, Direction.NORTH, 0);
        helper.runAfterDelay(10, () -> {
            for (BlockPos pos : new BlockPos[] {access, outbound, garage}) {
                helper.assertTrue(!sameWarehouse(helper, controller, pos), "no rail, no warehouse at " + pos);
                helper.assertTrue(!WarehouseFixtures.lit(helper, pos), "the lamp is dark at " + pos);
            }
            helper.succeed();
        });
    }
}
