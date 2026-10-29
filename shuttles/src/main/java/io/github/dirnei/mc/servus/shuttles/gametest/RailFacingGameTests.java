package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.station.StationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Stations, Fluid stations and the Field Head turn their front to a rail when placed; sneaking faces the player. */
public final class RailFacingGameTests {

    private static final BlockPos SPOT = new BlockPos(3, 1, 3);

    private RailFacingGameTests() {
    }

    /** Looking north across a rail, a station placed just past it faces south, towards the rail. */
    @ServusGameTest
    public static void stationFromTheAisleFacesTheRail(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.south());
        Placing.onTop(helper, new ItemStack(ServusShuttlesItems.INBOUND_STATION.get()), SPOT.below(), Direction.NORTH, false);
        helper.assertBlockProperty(SPOT, StationBlock.FACING, Direction.SOUTH);
        helper.succeed();
    }

    /** Clicking the east side of a rail puts the station east of it, facing west. */
    @ServusGameTest
    public static void stationClickedOnRailSideFacesIt(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.west());
        Placing.use(helper, new ItemStack(ServusShuttlesItems.OUTBOUND_STATION.get()), SPOT.west(), Direction.EAST, Direction.NORTH, false);
        helper.assertBlockProperty(SPOT, StationBlock.FACING, Direction.WEST);
        helper.succeed();
    }

    /** Rails north and west: the one where the player looks wins. */
    @ServusGameTest
    public static void stationRailInLookDirectionWins(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.west());
        WarehouseFixtures.rail(helper, SPOT.north());
        Placing.onTop(helper, new ItemStack(ServusShuttlesItems.PACKING_STATION.get()), SPOT.below(), Direction.NORTH, false);
        helper.assertBlockProperty(SPOT, StationBlock.FACING, Direction.NORTH);
        helper.succeed();
    }

    /** Rails east and west, none where the player looks: the front faces the player. */
    @ServusGameTest
    public static void stationBetweenRailsFacesThePlayer(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.west());
        WarehouseFixtures.rail(helper, SPOT.east());
        Placing.onTop(helper, new ItemStack(ServusShuttlesItems.INBOUND_STATION.get()), SPOT.below(), Direction.NORTH, false);
        helper.assertBlockProperty(SPOT, StationBlock.FACING, Direction.SOUTH);
        helper.succeed();
    }

    /** Sneaking ignores the rail beside the station: its front faces the player. */
    @ServusGameTest
    public static void sneakingStationFacesThePlayer(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.west());
        Placing.onTop(helper, new ItemStack(ServusShuttlesItems.INBOUND_STATION.get()), SPOT.below(), Direction.NORTH, true);
        helper.assertBlockProperty(SPOT, StationBlock.FACING, Direction.SOUTH);
        helper.succeed();
    }

    /** A Warehouse Controller placed from the aisle turns its front to the rail beside it; sneaking faces the player. */
    @ServusGameTest
    public static void controllerFacesTheRail(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.west());
        Placing.onTop(helper, new ItemStack(ServusShuttlesItems.WAREHOUSE_CONTROLLER.get()), SPOT.below(), Direction.NORTH, false);
        helper.assertBlockProperty(SPOT, WarehouseControllerBlock.FACING, Direction.WEST);
        BlockPos other = new BlockPos(3, 1, 1);
        WarehouseFixtures.rail(helper, other.west());
        Placing.onTop(helper, new ItemStack(ServusShuttlesItems.WAREHOUSE_CONTROLLER.get()), other.below(), Direction.NORTH, true);
        helper.assertBlockProperty(other, WarehouseControllerBlock.FACING, Direction.SOUTH);
        helper.succeed();
    }

    /** A Warehouse Rail west and a Farm Rail north, the player looking east: the Field Head faces the Farm Rail. */
    @ServusGameTest
    public static void fieldHeadPicksTheFarmRail(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.west());
        FieldFixtures.farmRail(helper, SPOT.north());
        Placing.onTop(helper, new ItemStack(ServusShuttlesItems.FIELD_HEAD.get()), SPOT.below(), Direction.EAST, false);
        helper.assertBlockProperty(SPOT, FieldHeadBlock.FACING, Direction.NORTH);
        helper.succeed();
    }

    /** A Field Head beside only a Warehouse Rail is not placed; with a Farm Rail too, or no rail at all, it is. */
    @ServusGameTest
    public static void fieldHeadRefusesWarehouseRailOnly(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.west());
        Placing.onTop(helper, new ItemStack(ServusShuttlesItems.FIELD_HEAD.get()), SPOT.below(), Direction.EAST, false);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.FIELD_HEAD.get(), SPOT);
        FieldFixtures.farmRail(helper, SPOT.north());
        Placing.onTop(helper, new ItemStack(ServusShuttlesItems.FIELD_HEAD.get()), SPOT.below(), Direction.EAST, false);
        helper.assertBlockProperty(SPOT, FieldHeadBlock.FACING, Direction.NORTH);
        BlockPos alone = new BlockPos(1, 1, 5);
        Placing.onTop(helper, new ItemStack(ServusShuttlesItems.FIELD_HEAD.get()), alone.below(), Direction.EAST, false);
        helper.assertBlockPresent(ServusShuttlesBlocks.FIELD_HEAD.get(), alone);
        helper.succeed();
    }

    /** A sneak placed head with its back to the Farm Rail faces the player and still serves that rail. */
    @ServusGameTest
    public static void sneakPlacedFieldHeadStillServes(GameTestHelper helper) {
        floor(helper);
        FieldFixtures.farmRail(helper, SPOT.north());
        Placing.onTop(helper, new ItemStack(ServusShuttlesItems.FIELD_HEAD.get()), SPOT.below(), Direction.NORTH, true);
        helper.assertBlockProperty(SPOT, FieldHeadBlock.FACING, Direction.SOUTH);
        FieldHeadBlockEntity head = helper.getBlockEntity(SPOT, FieldHeadBlockEntity.class);
        helper.assertTrue(helper.absolutePos(SPOT.north()).equals(head.headRail()), "the head serves the Farm Rail behind it, got " + head.headRail());
        helper.succeed();
    }

    /** A Fluid Inbound with a rail only beside its lower half's west side faces west, both halves. */
    @ServusGameTest
    public static void fluidInboundFacesTheRail(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.west());
        Placing.onTop(helper, FluidStationFixtures.item(), SPOT.below(), Direction.NORTH, false);
        assertFluidStation(helper, Direction.WEST);
        helper.succeed();
    }

    /** A Fluid Outbound with a rail only beside its lower half's west side faces west, both halves. */
    @ServusGameTest
    public static void fluidOutboundFacesTheRail(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.west());
        Placing.onTop(helper, FluidStationFixtures.outboundItem(), SPOT.below(), Direction.NORTH, false);
        assertFluidStation(helper, Direction.WEST);
        helper.succeed();
    }

    private static void assertFluidStation(GameTestHelper helper, Direction facing) {
        helper.assertBlockProperty(SPOT, FluidStationBlock.HALF, DoubleBlockHalf.LOWER);
        helper.assertBlockProperty(SPOT, FluidStationBlock.FACING, facing);
        helper.assertBlockProperty(SPOT.above(), FluidStationBlock.FACING, facing);
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                WarehouseFixtures.stone(helper, new BlockPos(x, 0, z));
            }
        }
    }
}
