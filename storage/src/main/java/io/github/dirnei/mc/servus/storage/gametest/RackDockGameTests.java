package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.HeavyRackMenu;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

/** A Heavy Duty Rack is a chest for hoppers and pipes only while no warehouse rail docks to its section. */
public final class RackDockGameTests {

    private static final BlockPos RACK = new BlockPos(5, 1, 5);
    /** Rows of racks placed by hand face south; the rail goes in front, at z + 1. */
    private static final int ROW_Z = 4;

    private RackDockGameTests() {
    }

    private static boolean open(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), Direction.UP) != null;
    }

    /** Places a rack the way a player facing north would: it faces south and joins the bays beside it. */
    private static void placeByHand(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        ItemStack rackItem = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static Player facingNorth(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(180.0F);
        return player;
    }

    private static HopperBlockEntity hopperAbove(GameTestHelper helper, BlockPos pos, int bread) {
        helper.setBlock(pos.above(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(pos.above(), HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.BREAD, bread));
        return hopper;
    }

    /** A rack without a rail is a big chest: a hopper above fills it. */
    @ServusGameTest(timeoutTicks = 100)
    public static void hopperFillsStandaloneRack(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        HopperBlockEntity hopper = hopperAbove(helper, RACK, 10);
        HeavyRackBlockEntity rack = helper.getBlockEntity(RACK, HeavyRackBlockEntity.class);
        Waits.when(helper, () -> WarehouseFixtures.count(rack, Items.BREAD) == 10 && hopper.isEmpty(), 80,
                () -> "the hopper should fill the rack, it holds " + WarehouseFixtures.count(rack, Items.BREAD), helper::succeed);
    }

    /** A warehouse rail laid in front closes the rack: no capability, and a hopper above stops. */
    @ServusGameTest(timeoutTicks = 100)
    public static void railDockClosesRack(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, new BlockPos(3, 1, 6), false);
        BlockPos rackPos = warehouse.rackPos();
        helper.assertTrue(!open(helper, rackPos), "the mini warehouse's rack is docked and closed");
        helper.setBlock(warehouse.railPos(0), Blocks.AIR.defaultBlockState());
        helper.assertTrue(open(helper, rackPos), "without its rail the rack opens");
        WarehouseFixtures.railOnStone(helper, warehouse.railPos(0));
        helper.assertTrue(!open(helper, rackPos), "the rail closes it again");
        HopperBlockEntity hopper = hopperAbove(helper, rackPos, 10);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 10, "the hopper above a docked rack keeps its bread");
            helper.assertTrue(warehouse.rack().isEmpty(), "the docked rack stays empty");
            helper.succeed();
        });
    }

    /** Breaking the rail in front opens the rack to a hopper again. */
    @ServusGameTest(timeoutTicks = 100)
    public static void railRemovedOpensRack(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        WarehouseFixtures.railOnStone(helper, RACK.north());
        helper.assertTrue(!open(helper, RACK), "a rail in front closes the rack");
        HopperBlockEntity hopper = hopperAbove(helper, RACK, 6);
        HeavyRackBlockEntity rack = helper.getBlockEntity(RACK, HeavyRackBlockEntity.class);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 6, "nothing goes in while docked");
            helper.setBlock(RACK.north(), Blocks.AIR.defaultBlockState());
            helper.assertTrue(open(helper, RACK), "without the rail the rack is open");
            Waits.when(helper, () -> WarehouseFixtures.count(rack, Items.BREAD) == 6, 60,
                    () -> "the hopper should fill the rack again, it holds " + WarehouseFixtures.count(rack, Items.BREAD), helper::succeed);
        });
    }

    /** A rail in front of one bay closes the whole section of three. */
    @ServusGameTest(timeoutTicks = 40)
    public static void oneDockedBayClosesSection(GameTestHelper helper) {
        Player player = facingNorth(helper);
        for (int x = 2; x <= 4; x++) {
            placeByHand(helper, player, new BlockPos(x, 1, ROW_Z));
        }
        helper.assertTrue(HeavyRackBlock.section(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, ROW_Z))).size() == 3, "the three racks form one section");
        for (int x = 2; x <= 4; x++) {
            helper.assertTrue(open(helper, new BlockPos(x, 1, ROW_Z)), "without a rail bay " + x + " is open");
        }
        WarehouseFixtures.railOnStone(helper, new BlockPos(4, 1, ROW_Z + 1));
        for (int x = 2; x <= 4; x++) {
            helper.assertTrue(!open(helper, new BlockPos(x, 1, ROW_Z)), "one rail closes bay " + x);
        }
        helper.succeed();
    }

    /** A bay joining a docked section is closed; once the docked bay goes, the rest opens again. */
    @ServusGameTest(timeoutTicks = 40)
    public static void joiningDockedSectionCloses(GameTestHelper helper) {
        Player player = facingNorth(helper);
        placeByHand(helper, player, new BlockPos(4, 1, ROW_Z));
        WarehouseFixtures.railOnStone(helper, new BlockPos(4, 1, ROW_Z + 1));
        placeByHand(helper, player, new BlockPos(3, 1, ROW_Z));
        helper.assertTrue(HeavyRackBlock.section(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, ROW_Z))).size() == 2, "the new bay joins the section");
        helper.assertTrue(!open(helper, new BlockPos(3, 1, ROW_Z)), "the joining bay is closed");
        helper.setBlock(new BlockPos(4, 1, ROW_Z), Blocks.AIR.defaultBlockState());
        helper.assertTrue(open(helper, new BlockPos(3, 1, ROW_Z)), "with the docked bay gone the rest opens");
        helper.succeed();
    }

    /** A duct's item arm toward a rack disappears when a rail docks and comes back when it is broken. */
    @ServusGameTest(timeoutTicks = 60)
    public static void ductArmFollowsDock(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        BlockPos ductPos = RACK.east();
        GameTestSteps.duct(helper, ductPos, Line.ITEM);
        UtilityDuctBlockEntity duct = helper.getBlockEntity(ductPos, UtilityDuctBlockEntity.class);
        duct.recomputeArms();
        helper.assertTrue(duct.hasArm(Line.ITEM, Direction.WEST), "the duct shows an arm toward the open rack");
        WarehouseFixtures.railOnStone(helper, RACK.north());
        Waits.when(helper, () -> !duct.hasArm(Line.ITEM, Direction.WEST), 10, () -> "the arm should go once a rail docks", () -> {
            helper.setBlock(RACK.north(), Blocks.AIR.defaultBlockState());
            Waits.when(helper, () -> duct.hasArm(Line.ITEM, Direction.WEST), 10, () -> "the arm should come back without the rail", helper::succeed);
        });
    }

    /** A rail closes the rack even when it belongs to no online warehouse. */
    @ServusGameTest(timeoutTicks = 20)
    public static void railWithoutControllerCloses(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        WarehouseFixtures.railOnStone(helper, RACK.north());
        helper.assertTrue(!open(helper, RACK), "a rail without a controller still closes the rack");
        helper.assertTrue(!helper.getBlockEntity(RACK, HeavyRackBlockEntity.class).standalone(), "the rack is not standalone");
        helper.succeed();
    }

    /** A docked rack still opens its GUI by hand and takes items there. */
    @ServusGameTest(timeoutTicks = 20)
    public static void handGuiOnDockedRack(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        WarehouseFixtures.railOnStone(helper, RACK.north());
        HeavyRackBlockEntity rack = helper.getBlockEntity(RACK, HeavyRackBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos front = helper.absolutePos(RACK.north().north());
        player.setPos(front.getX() + 0.5, front.getY(), front.getZ() + 0.5);
        HeavyRackMenu menu = HeavyRackMenu.forSection(1, player.getInventory(), rack);
        helper.assertTrue(menu.stillValid(player), "the menu opens on a docked rack");
        player.getInventory().setItem(0, new ItemStack(Items.APPLE, 12));
        menu.quickMoveStack(player, menu.rackSlots() + 27);
        helper.assertTrue(WarehouseFixtures.count(rack, Items.APPLE) == 12, "shift click moves the apples into the docked rack");
        helper.succeed();
    }
}
