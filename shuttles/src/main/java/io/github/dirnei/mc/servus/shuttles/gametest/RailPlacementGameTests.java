package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.Fluids;

/** Placing rails: hanging on rack fronts, bridges, overhangs and sneak placing. */
public final class RailPlacementGameTests {

    private static final int HANG_Y = 3;

    private RailPlacementGameTests() {
    }

    private static void hangRack(GameTestHelper helper, int x, int z, Direction facing) {
        WarehouseFixtures.rack(helper, new BlockPos(x, HANG_Y, z), facing);
    }

    private static void hangRail(GameTestHelper helper, int x, int z) {
        helper.setBlock(new BlockPos(x, HANG_Y, z), ServusShuttlesBlocks.RAIL.get().defaultBlockState());
    }

    private static boolean railAt(GameTestHelper helper, int x, int z) {
        return helper.getBlockState(new BlockPos(x, HANG_Y, z)).getBlock() instanceof RailBlock;
    }

    /** Uses a rail item on the given face of a block, like a player clicking it. */
    private static void clickRail(GameTestHelper helper, Player player, BlockPos clicked, Direction face) {
        BlockPos absolute = helper.absolutePos(clicked);
        ItemStack rail = new ItemStack(ServusShuttlesBlocks.RAIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rail);
        rail.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false)));
    }

    /** A rail hangs on a raised rack front; the rack side, a controller and open air take none. */
    @ServusGameTest(timeoutTicks = 40)
    public static void railHangsOnRackFrontOnly(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        hangRack(helper, 2, 2, Direction.SOUTH);
        WarehouseFixtures.controller(helper, new BlockPos(8, HANG_Y, 2));
        clickRail(helper, player, new BlockPos(2, HANG_Y, 2), Direction.SOUTH);
        clickRail(helper, player, new BlockPos(2, HANG_Y, 2), Direction.EAST);
        clickRail(helper, player, new BlockPos(8, HANG_Y, 2), Direction.SOUTH);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(railAt(helper, 2, 3), "a rail should hang on the rack front");
            helper.assertFalse(railAt(helper, 3, 2), "the rack side is no anchor");
            helper.assertFalse(railAt(helper, 8, 3), "a controller is no anchor");
            helper.succeed();
        });
    }

    /** A row of rails hangs along five raised racks at any length. */
    @ServusGameTest(timeoutTicks = 40)
    public static void hungRowAlongRacksHolds(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            hangRack(helper, x, 2, Direction.SOUTH);
            hangRail(helper, x, 3);
        }
        helper.runAfterDelay(5, () -> {
            for (int x = 1; x <= 5; x++) {
                helper.assertTrue(railAt(helper, x, 3), "rail " + x + " of the row should hang");
            }
            helper.succeed();
        });
    }

    /** Three rails bridge two elevator shafts, ends first, and link them. */
    @ServusGameTest(timeoutTicks = 40)
    public static void bridgeBetweenElevatorsHolds(GameTestHelper helper) {
        for (int x : new int[] {1, 5}) {
            helper.setBlock(new BlockPos(x, HANG_Y - 1, 2), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, HANG_Y, 2), ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        }
        hangRail(helper, 2, 2);
        hangRail(helper, 4, 2);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(railAt(helper, 2, 2) && railAt(helper, 4, 2), "a rail should hang on each shaft");
            hangRail(helper, 3, 2);
            helper.runAfterDelay(5, () -> {
                for (int x = 2; x <= 4; x++) {
                    helper.assertTrue(railAt(helper, x, 2), "bridge rail " + x + " should stay");
                }
                helper.succeed();
            });
        });
    }

    /** Three rails bridge two raised racks facing each other; breaking one rack drops the bridge. */
    @ServusGameTest(timeoutTicks = 40)
    public static void bridgeBetweenRacksFallsWithItsRack(GameTestHelper helper) {
        hangRack(helper, 1, 2, Direction.EAST);
        hangRack(helper, 5, 2, Direction.WEST);
        hangRail(helper, 2, 2);
        hangRail(helper, 4, 2);
        helper.runAfterDelay(3, () -> {
            hangRail(helper, 3, 2);
            helper.runAfterDelay(5, () -> {
                for (int x = 2; x <= 4; x++) {
                    helper.assertTrue(railAt(helper, x, 2), "bridge rail " + x + " should stay");
                }
                helper.setBlock(new BlockPos(5, HANG_Y, 2), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(5, () -> {
                    for (int x = 2; x <= 4; x++) {
                        helper.assertFalse(railAt(helper, x, 2), "bridge rail " + x + " should fall with its rack");
                    }
                    helper.succeed();
                });
            });
        });
    }

    /** A fourth rail beside the middle of a three rail bridge drops all four. */
    @ServusGameTest(timeoutTicks = 40)
    public static void fourthRailCollapsesBridge(GameTestHelper helper) {
        hangRack(helper, 1, 2, Direction.EAST);
        hangRack(helper, 5, 2, Direction.WEST);
        hangRail(helper, 2, 2);
        hangRail(helper, 4, 2);
        helper.runAfterDelay(3, () -> {
            hangRail(helper, 3, 2);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(railAt(helper, 2, 2) && railAt(helper, 3, 2) && railAt(helper, 4, 2), "three rails should hold");
                hangRail(helper, 3, 3);
                helper.runAfterDelay(5, () -> {
                    helper.assertFalse(railAt(helper, 2, 2) || railAt(helper, 3, 2) || railAt(helper, 4, 2) || railAt(helper, 3, 3),
                            "all four rails should have collapsed");
                    helper.succeed();
                });
            });
        });
    }

    /** In a four block gap the first free rail drops with the hung rail it touches; the far hung rail stays. */
    @ServusGameTest(timeoutTicks = 40)
    public static void fourBlockGapCannotBeBridged(GameTestHelper helper) {
        hangRack(helper, 1, 2, Direction.EAST);
        hangRack(helper, 6, 2, Direction.WEST);
        hangRail(helper, 2, 2);
        hangRail(helper, 5, 2);
        helper.runAfterDelay(3, () -> {
            hangRail(helper, 3, 2);
            helper.runAfterDelay(5, () -> {
                helper.assertFalse(railAt(helper, 2, 2) || railAt(helper, 3, 2), "the overhang should drop");
                helper.assertTrue(railAt(helper, 5, 2), "the far hung rail should stay");
                helper.succeed();
            });
        });
    }

    /** Two floor edges three blocks apart are bridged by one rail off each edge and one in the middle. */
    @ServusGameTest(timeoutTicks = 40)
    public static void bridgeBetweenFloorsHolds(GameTestHelper helper) {
        for (int x : new int[] {1, 5}) {
            helper.setBlock(new BlockPos(x, HANG_Y - 1, 2), Blocks.STONE.defaultBlockState());
            hangRail(helper, x, 2);
        }
        hangRail(helper, 2, 2);
        hangRail(helper, 4, 2);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(railAt(helper, 2, 2) && railAt(helper, 4, 2), "a rail should hang off each floor edge");
            hangRail(helper, 3, 2);
            helper.runAfterDelay(5, () -> {
                for (int x = 1; x <= 5; x++) {
                    helper.assertTrue(railAt(helper, x, 2), "rail " + x + " should stay");
                }
                helper.setBlock(new BlockPos(5, HANG_Y - 1, 2), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(5, () -> {
                    helper.assertTrue(railAt(helper, 1, 2), "the standing rail should stay");
                    for (int x = 2; x <= 5; x++) {
                        helper.assertFalse(railAt(helper, x, 2), "rail " + x + " should fall with the floor");
                    }
                    helper.succeed();
                });
            });
        });
    }

    /** A free rail beside a hung rail and nothing else drops both. */
    @ServusGameTest(timeoutTicks = 40)
    public static void overhangCollapses(GameTestHelper helper) {
        hangRack(helper, 1, 2, Direction.EAST);
        hangRail(helper, 2, 2);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(railAt(helper, 2, 2), "the hung rail should stay on its own");
            hangRail(helper, 3, 2);
            helper.runAfterDelay(5, () -> {
                helper.assertFalse(railAt(helper, 2, 2) || railAt(helper, 3, 2), "the overhang should drop both rails");
                helper.succeed();
            });
        });
    }

    /** Breaking the floor under a rail leaves it hung on a rack front and drops a lone rail. */
    @ServusGameTest(timeoutTicks = 40)
    public static void floorRemovedUnderRails(GameTestHelper helper) {
        hangRack(helper, 2, 2, Direction.SOUTH);
        helper.setBlock(new BlockPos(2, HANG_Y - 1, 3), Blocks.STONE.defaultBlockState());
        hangRail(helper, 2, 3);
        helper.setBlock(new BlockPos(8, HANG_Y - 1, 3), Blocks.STONE.defaultBlockState());
        hangRail(helper, 8, 3);
        helper.runAfterDelay(3, () -> {
            helper.setBlock(new BlockPos(2, HANG_Y - 1, 3), Blocks.AIR.defaultBlockState());
            helper.setBlock(new BlockPos(8, HANG_Y - 1, 3), Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(railAt(helper, 2, 3), "the rail in front of the rack should stay hung");
                helper.assertFalse(railAt(helper, 8, 3), "the lone rail should drop");
                helper.succeed();
            });
        });
    }

    /** Uses a rail item while sneaking, hitting the given face at a local point (top and bottom faces use x and z). */
    private static ItemStack sneakRail(GameTestHelper helper, Player player, BlockPos clicked, Direction face, double x, double z) {
        BlockPos absolute = helper.absolutePos(clicked);
        ItemStack rail = new ItemStack(ServusShuttlesBlocks.RAIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rail);
        player.setShiftKeyDown(true);
        Vec3 location = face.getAxis().isVertical()
                ? new Vec3(absolute.getX() + x, absolute.getY() + (face == Direction.UP ? 2.0 / 16.0 : 0.0), absolute.getZ() + z)
                : Vec3.atCenterOf(absolute).relative(face, 0.5);
        rail.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(location, face, absolute, false)));
        player.setShiftKeyDown(false);
        return rail;
    }

    private static boolean railAt(GameTestHelper helper, int x, int y, int z) {
        return helper.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof RailBlock;
    }

    private static void stoneFloor(GameTestHelper helper, int z) {
        for (int x = 0; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
        }
    }

    private static void floorRails(GameTestHelper helper, int fromX, int toX, int z) {
        stoneFloor(helper, z);
        for (int x = fromX; x <= toX; x++) {
            WarehouseFixtures.rail(helper, new BlockPos(x, 2, z));
        }
    }

    /** Sneaking with a rail on the east half of a floor run places past its end; a plain click still turns. */
    @ServusGameTest(timeoutTicks = 40)
    public static void sneakRailExtendsStraight(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        floorRails(helper, 1, 4, 3);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE.defaultBlockState());
        sneakRail(helper, player, new BlockPos(1, 2, 3), Direction.UP, 0.9, 0.5);
        helper.assertTrue(railAt(helper, 5, 2, 3), "the rail should land past the fourth rail");
        helper.assertFalse(railAt(helper, 1, 3, 3), "nothing should land on top of the clicked rail");
        clickRail(helper, player, new BlockPos(1, 2, 3), Direction.NORTH);
        helper.assertTrue(railAt(helper, 1, 2, 2), "a plain click should place against the north face");
        helper.assertFalse(railAt(helper, 6, 2, 3), "a plain click should not walk");
        helper.succeed();
    }

    /** On lone rails the top face quadrant and side faces pick the direction; the walk passes a junction. */
    @ServusGameTest(timeoutTicks = 40)
    public static void sneakRailDirectionAndJunction(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        floorRails(helper, 1, 4, 3);
        stoneFloor(helper, 1);
        stoneFloor(helper, 2);
        WarehouseFixtures.rail(helper, new BlockPos(2, 2, 2));
        sneakRail(helper, player, new BlockPos(1, 2, 3), Direction.UP, 0.85, 0.4);
        helper.assertTrue(railAt(helper, 5, 2, 3), "the walk should pass the junction and land past the line");
        WarehouseFixtures.rail(helper, new BlockPos(8, 2, 3));
        sneakRail(helper, player, new BlockPos(8, 2, 3), Direction.UP, 0.5, 0.1);
        helper.assertTrue(railAt(helper, 8, 2, 2), "a hit near the north edge of a lone rail should place north");
        WarehouseFixtures.rail(helper, new BlockPos(0, 2, 1));
        sneakRail(helper, player, new BlockPos(0, 2, 1), Direction.SOUTH, 0.5, 0.5);
        helper.assertTrue(railAt(helper, 0, 2, 2), "a side face hit on a lone rail should walk towards that face");
        helper.succeed();
    }

    /** A hit near the north edge of a rail inside an east west line still extends the line by the half it lands in. */
    @ServusGameTest(timeoutTicks = 40)
    public static void sneakRailFollowsStraightRun(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        floorRails(helper, 1, 4, 3);
        stoneFloor(helper, 2);
        sneakRail(helper, player, new BlockPos(2, 2, 3), Direction.UP, 0.6, 0.1);
        helper.assertTrue(railAt(helper, 5, 2, 3), "the rail should land past the east end of the line");
        helper.assertFalse(railAt(helper, 2, 2, 2), "no rail should land north of the clicked rail");
        sneakRail(helper, player, new BlockPos(3, 2, 3), Direction.NORTH, 0.5, 0.5);
        helper.assertTrue(railAt(helper, 6, 2, 3), "a side face hit across the line follows the line too");
        helper.succeed();
    }

    /** A run that ends at a stone block places nothing and keeps the stack. */
    @ServusGameTest(timeoutTicks = 40)
    public static void sneakRailBlockedPlacesNothing(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        floorRails(helper, 1, 3, 3);
        helper.setBlock(new BlockPos(4, 2, 3), Blocks.STONE.defaultBlockState());
        ItemStack held = sneakRail(helper, player, new BlockPos(1, 2, 3), Direction.UP, 0.9, 0.5);
        helper.assertTrue(held.getCount() == 1, "the stack should keep its rail");
        helper.assertFalse(railAt(helper, 1, 3, 3) || railAt(helper, 1, 2, 2) || railAt(helper, 1, 2, 4),
                "no rail should be placed anywhere");
        helper.succeed();
    }

    /** Sneak clicking a rail hung on a rack towards the other rack fills the middle of the gap and the bridge holds. */
    @ServusGameTest(timeoutTicks = 40)
    public static void sneakRailBridgesRacks(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        hangRack(helper, 1, 2, Direction.EAST);
        hangRack(helper, 5, 2, Direction.WEST);
        hangRail(helper, 2, 2);
        hangRail(helper, 4, 2);
        helper.runAfterDelay(3, () -> {
            sneakRail(helper, player, new BlockPos(2, HANG_Y, 2), Direction.UP, 0.9, 0.5);
            helper.runAfterDelay(5, () -> {
                for (int x = 2; x <= 4; x++) {
                    helper.assertTrue(railAt(helper, x, 2), "bridge rail " + x + " should stay");
                }
                helper.succeed();
            });
        });
    }

    /** Sneak extending a lone hung rail into the air drops both, as a hand placed overhang would. */
    @ServusGameTest(timeoutTicks = 40)
    public static void sneakRailOverhangCollapses(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        hangRack(helper, 1, 2, Direction.EAST);
        hangRail(helper, 2, 2);
        helper.runAfterDelay(3, () -> {
            sneakRail(helper, player, new BlockPos(2, HANG_Y, 2), Direction.UP, 0.9, 0.5);
            helper.assertTrue(railAt(helper, 3, 2), "the rail should be placed beyond the hung rail");
            helper.runAfterDelay(5, () -> {
                helper.assertFalse(railAt(helper, 2, 2) || railAt(helper, 3, 2), "the overhang should drop both rails");
                helper.succeed();
            });
        });
    }

    private static void shaft(GameTestHelper helper, int x, int z, int height) {
        helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
        for (int y = 1; y <= height; y++) {
            helper.setBlock(new BlockPos(x, y, z), ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        }
    }

    /** Centre of the fluid tests, far enough from the edges that water does not run off. */
    private static final BlockPos POOL = new BlockPos(6, 1, 6);

    /** Stone under the whole area, so fluids spread evenly instead of running to an edge. */
    static void stoneFloor(GameTestHelper helper) {
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
    }

    /** Water flowing against a rail and a farm rail leaves both standing and dry. */
    @ServusGameTest(timeoutTicks = 100)
    public static void waterStopsAtRail(GameTestHelper helper) {
        stoneFloor(helper);
        helper.setBlock(POOL.east(), ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(POOL.west(), ServusShuttlesBlocks.FARM_RAIL.get().defaultBlockState());
        helper.setBlock(POOL, Blocks.WATER.defaultBlockState());
        helper.runAfterDelay(40, () -> {
            helper.assertBlockPresent(ServusShuttlesBlocks.RAIL.get(), POOL.east());
            helper.assertBlockPresent(ServusShuttlesBlocks.FARM_RAIL.get(), POOL.west());
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(POOL.east())).isEmpty(), "no water enters the rail");
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(POOL.west())).isEmpty(), "no water enters the farm rail");
            helper.assertFalse(helper.getLevel().getFluidState(helper.absolutePos(POOL.north())).isEmpty(), "the water flowed beside the rails");
            helper.succeed();
        });
    }

    /** A rail used on the floor of a water source places no rail and keeps the source. */
    @ServusGameTest(timeoutTicks = 20)
    public static void railRefusedInWaterSource(GameTestHelper helper) {
        stoneFloor(helper);
        helper.setBlock(POOL, Blocks.WATER.defaultBlockState());
        clickRail(helper, helper.makeMockPlayer(GameType.SURVIVAL), POOL.below(), Direction.UP);
        helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(POOL)).isSourceOfType(Fluids.WATER), "the water source stays");
        helper.assertFalse(helper.getBlockState(POOL).getBlock() instanceof RailBlock, "no rail is placed");
        helper.succeed();
    }
}
