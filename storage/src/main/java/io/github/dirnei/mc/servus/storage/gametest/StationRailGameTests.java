package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlock;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlock;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Shuttle served blocks join the warehouse only through rails. */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StationRailGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos STATION = new BlockPos(3, 1, 2);
    private static final BlockPos CRATE = new BlockPos(2, 1, 3);

    private StationRailGameTests() {
    }

    private static WarehouseControllerBlockEntity controller(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(CONTROLLER);
    }

    private static CrateBlockEntity crate(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos);
    }

    private static boolean lit(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(StorageNodeBlocks.ONLINE);
    }

    private static boolean sameWarehouse(GameTestHelper helper, WarehouseControllerBlockEntity controller, BlockPos pos) {
        StorageNodeBlockEntity node = helper.getBlockEntity(pos);
        return node.network().isPresent() && node.network().equals(controller.network());
    }

    /** A station touching the controller stays out until a rail links it, and leaves again when the rail breaks. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stationJoinsOnlyByRail(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = controller(helper);
        CrateBlockEntity crate = crate(helper, CRATE);
        helper.setBlock(STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        InboundStationBlockEntity station = helper.getBlockEntity(STATION);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!sameWarehouse(helper, controller, STATION), "touching the controller is not enough");
            helper.assertTrue(!lit(helper, STATION), "the lamp is dark without a rail");
            ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 64), false);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!station.isEmpty() && crate.count() == 0, "the cobblestone waits in the buffer");
                TerminalWiring.railUnder(helper, STATION);
                TerminalWiring.when(helper, () -> crate.count() == 64 && station.isEmpty(), 60,
                        () -> "with a rail the cobblestone is put away, crate has " + crate.count(), () -> {
                    helper.assertTrue(sameWarehouse(helper, controller, STATION) && lit(helper, STATION), "the rail links the station");
                    helper.setBlock(STATION.below(), Blocks.AIR.defaultBlockState());
                    TerminalWiring.when(helper, () -> !lit(helper, STATION), 20,
                            () -> "breaking the rail takes the station out", () -> {
                        helper.assertTrue(!sameWarehouse(helper, controller, STATION), "the station left the warehouse");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** A crate touching only a rail linked station does not join through it. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void crateBesideStationNotLinked(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = controller(helper);
        helper.setBlock(STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        TerminalWiring.railUnder(helper, STATION);
        BlockPos beyond = STATION.east();
        CrateBlockEntity bread = crate(helper, beyond);
        bread.insert(new ItemStack(Items.BREAD, 12), false);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(sameWarehouse(helper, controller, STATION), "the station is linked by rail");
            long count = controller.network().orElseThrow().index().count(ItemKey.of(new ItemStack(Items.BREAD)));
            helper.assertTrue(count == 0, "bread beyond the station is not stock, counted " + count);
            helper.assertTrue(!lit(helper, beyond), "the crate's lamp stays dark");
            helper.succeed();
        });
    }

    /** Access Point, Outbound Station and Shuttle Garage pressed against the controller stay out without a rail. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void shuttleBlocksNeedARail(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = controller(helper);
        BlockPos access = CONTROLLER.east();
        BlockPos outbound = CONTROLLER.west();
        BlockPos garage = CONTROLLER.south();
        helper.setBlock(access, ServusStorageBlocks.ACCESS_POINT.get().defaultBlockState());
        helper.setBlock(outbound, ServusStorageBlocks.OUTBOUND_STATION.get().defaultBlockState()
                .setValue(OutboundStationBlock.FACING, Direction.WEST));
        helper.setBlock(garage, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState()
                .setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(10, () -> {
            for (BlockPos pos : new BlockPos[] {access, outbound, garage}) {
                helper.assertTrue(!sameWarehouse(helper, controller, pos), "no rail, no warehouse at " + pos);
                helper.assertTrue(!lit(helper, pos), "the lamp is dark at " + pos);
            }
            helper.succeed();
        });
    }
}
