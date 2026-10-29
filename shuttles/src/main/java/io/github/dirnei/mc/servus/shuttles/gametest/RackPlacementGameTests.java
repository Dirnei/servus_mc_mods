package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Racks copy the facing of the rack they are placed against, else turn to a rail; sneaking extends a row. */
public final class RackPlacementGameTests {

    private static final BlockPos RACK = new BlockPos(3, 1, 3);

    private RackPlacementGameTests() {
    }

    /** Clicking the east side of a rack facing north puts a rack east of it, facing north. */
    @ServusGameTest
    public static void rackAgainstRackSideCopiesFacing(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        Placing.use(helper, heavyRack(), RACK, Direction.EAST, Direction.WEST, false);
        helper.assertBlockProperty(RACK.east(), HeavyRackBlock.FACING, Direction.NORTH);
        helper.succeed();
    }

    /** Clicking the front of a rack facing north, looking west, puts a rack in front of it, still facing north. */
    @ServusGameTest
    public static void rackAgainstRackFrontCopiesFacing(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        Placing.use(helper, heavyRack(), RACK, Direction.NORTH, Direction.WEST, false);
        helper.assertBlockProperty(RACK.north(), HeavyRackBlock.FACING, Direction.NORTH);
        helper.succeed();
    }

    /** Looking north across a rail, a rack placed on the floor beyond it faces south, towards the rail. */
    @ServusGameTest
    public static void rackFromTheAisleFacesTheRail(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, RACK.south());
        Placing.onTop(helper, heavyRack(), RACK.below(), Direction.NORTH, false);
        helper.assertBlockProperty(RACK, HeavyRackBlock.FACING, Direction.SOUTH);
        helper.succeed();
    }

    /** A Fluid Rack with a rail only on its west side faces west. */
    @ServusGameTest
    public static void fluidRackFacesTheRail(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, RACK.west());
        Placing.onTop(helper, FluidRackFixtures.item(), RACK.below(), Direction.NORTH, false);
        helper.assertBlockProperty(RACK, FluidRackBlock.FACING, Direction.WEST);
        helper.succeed();
    }

    /** A birch rack against the side of an oak rack copies its facing, though the two never join. */
    @ServusGameTest
    public static void otherWoodStillCopiesFacing(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        ItemStack birch = heavyRack();
        birch.set(ServusShuttlesComponents.RACK_WOOD.get(), BuiltInRegistries.BLOCK.getKey(Blocks.BIRCH_PLANKS));
        Placing.use(helper, birch, RACK, Direction.EAST, Direction.WEST, false);
        helper.assertBlockProperty(RACK.east(), HeavyRackBlock.FACING, Direction.NORTH);
        helper.assertBlockProperty(RACK.east(), HeavyRackBlock.LEFT, false);
        helper.succeed();
    }

    /** Sneaking at the west half of the front of the east end of a row of four walks west and places past the far end. */
    @ServusGameTest
    public static void sneakExtendsTheRowPastTheFarEnd(GameTestHelper helper) {
        floor(helper);
        row(helper, 2, 5);
        Player player = Placing.player(helper, Direction.SOUTH, true, heavyRack());
        Placing.use(helper, player, new BlockPos(5, 1, 3), Direction.NORTH, new Vec3(0.25, 0.5, 0));
        helper.assertBlockProperty(new BlockPos(1, 1, 3), HeavyRackBlock.FACING, Direction.NORTH);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), new BlockPos(5, 1, 2));
        helper.succeed();
    }

    /** Sneaking at the exposed west side of a row places right beside it, facing the row's way. */
    @ServusGameTest
    public static void sneakOnASideFacePlacesBesideIt(GameTestHelper helper) {
        floor(helper);
        row(helper, 2, 3);
        Player player = Placing.player(helper, Direction.EAST, true, heavyRack());
        Placing.use(helper, player, new BlockPos(2, 1, 3), Direction.WEST, new Vec3(0, 0.5, 0.5));
        helper.assertBlockProperty(new BlockPos(1, 1, 3), HeavyRackBlock.FACING, Direction.NORTH);
        helper.succeed();
    }

    /** Sneaking at the top of a column of two racks stacks a third on top, facing the column's way. */
    @ServusGameTest
    public static void sneakOnTheTopStacksUp(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rack(helper, new BlockPos(3, 1, 3), Direction.NORTH);
        WarehouseFixtures.rack(helper, new BlockPos(3, 2, 3), Direction.NORTH);
        Player player = Placing.player(helper, Direction.SOUTH, true, heavyRack());
        Placing.use(helper, player, new BlockPos(3, 1, 3), Direction.UP, new Vec3(0.25, 1, 0.5));
        helper.assertBlockProperty(new BlockPos(3, 3, 3), HeavyRackBlock.FACING, Direction.NORTH);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), new BlockPos(2, 1, 3));
        helper.succeed();
    }

    /** Sneaking at the top edge of the front of a rack stacks one on top of it, facing the same way. */
    @ServusGameTest
    public static void sneakAtTheTopEdgeOfAFrontStacksUp(GameTestHelper helper) {
        floor(helper);
        row(helper, 2, 4);
        Player player = Placing.player(helper, Direction.SOUTH, true, heavyRack());
        Placing.use(helper, player, new BlockPos(3, 1, 3), Direction.NORTH, new Vec3(0.5, 0.9, 0));
        helper.assertBlockProperty(new BlockPos(3, 2, 3), HeavyRackBlock.FACING, Direction.NORTH);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), new BlockPos(1, 1, 3));
        helper.succeed();
    }

    /** A row ending at stone takes no rack: nothing is placed and the item stays. */
    @ServusGameTest
    public static void sneakAtABlockedRowPlacesNothing(GameTestHelper helper) {
        floor(helper);
        row(helper, 2, 5);
        WarehouseFixtures.stone(helper, new BlockPos(1, 1, 3));
        ItemStack stack = heavyRack();
        Player player = Placing.player(helper, Direction.SOUTH, true, stack);
        Placing.use(helper, player, new BlockPos(5, 1, 3), Direction.NORTH, new Vec3(0.25, 0.5, 0));
        helper.assertTrue(stack.getCount() == 1, "the rack stays in the hand");
        helper.assertBlockNotPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), new BlockPos(5, 1, 2));
        helper.assertBlockNotPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), new BlockPos(6, 1, 3));
        helper.succeed();
    }

    /** Without sneaking, a click on the front of a row places against that face, not at the row's end. */
    @ServusGameTest
    public static void unsneakedClickPlacesAgainstTheFace(GameTestHelper helper) {
        floor(helper);
        row(helper, 2, 5);
        Player player = Placing.player(helper, Direction.SOUTH, false, heavyRack());
        Placing.use(helper, player, new BlockPos(5, 1, 3), Direction.NORTH, new Vec3(0.25, 0.5, 0));
        helper.assertBlockProperty(new BlockPos(5, 1, 2), HeavyRackBlock.FACING, Direction.NORTH);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), new BlockPos(1, 1, 3));
        helper.succeed();
    }

    /** Sneaking at the south half of the front of the north end of a Fluid Rack row facing east places past its south end. */
    @ServusGameTest
    public static void sneakExtendsAFluidRackRow(GameTestHelper helper) {
        floor(helper);
        FluidRackFixtures.row(helper, Direction.EAST, new BlockPos(3, 1, 3), new BlockPos(3, 1, 4));
        Player player = Placing.player(helper, Direction.WEST, true, FluidRackFixtures.item());
        Placing.use(helper, player, new BlockPos(3, 1, 3), Direction.EAST, new Vec3(1, 0.5, 0.75));
        helper.assertBlockProperty(new BlockPos(3, 1, 5), FluidRackBlock.FACING, Direction.EAST);
        helper.succeed();
    }

    /** Heavy Duty Racks facing north from x {@code fromX} to {@code toX} at z 3. */
    private static void row(GameTestHelper helper, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) {
            WarehouseFixtures.rack(helper, new BlockPos(x, 1, 3), Direction.NORTH);
        }
    }

    private static ItemStack heavyRack() {
        return new ItemStack(ServusShuttlesItems.HEAVY_RACK.get());
    }

    static void floor(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                WarehouseFixtures.stone(helper, new BlockPos(x, 0, z));
            }
        }
    }
}
