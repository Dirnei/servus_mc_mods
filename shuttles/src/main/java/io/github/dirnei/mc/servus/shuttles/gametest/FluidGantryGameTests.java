package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlock;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryPart;
import io.github.dirnei.mc.servus.shuttles.menu.FluidGantryMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Placing, refusing, breaking and placing back a Fluid Gantry, and its keep levels. */
public final class FluidGantryGameTests {

    private static final BlockPos RAIL = new BlockPos(4, 1, 4);

    private FluidGantryGameTests() {
    }

    private static boolean isPart(GameTestHelper helper, BlockPos pos, GantryPart part) {
        BlockState state = helper.getBlockState(pos);
        return state.is(ServusShuttlesBlocks.FLUID_GANTRY.get()) && state.getValue(FluidGantryBlock.PART) == part
                && state.getValue(FluidGantryBlock.AXIS) == Direction.Axis.Z;
    }

    private static boolean noGantryAround(GameTestHelper helper, BlockPos rail) {
        for (BlockPos pos : List.of(rail.west(), rail.west().above(), rail.east(), rail.east().above(), rail.above())) {
            if (helper.getBlockState(pos).is(ServusShuttlesBlocks.FLUID_GANTRY.get())) {
                return false;
            }
        }
        return true;
    }

    /** On a north to south rail the legs stand west and east, two tall, and the beam above the rail. */
    @ServusGameTest
    public static void gantryPlacedAroundStraightRail(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 6);
        ItemStack left = GantryFixtures.use(helper, RAIL, new ItemStack(ServusShuttlesItems.FLUID_GANTRY.get()));
        helper.assertTrue(isPart(helper, RAIL.west(), GantryPart.LEFT_LOW), "the master stands west of the rail");
        helper.assertTrue(isPart(helper, RAIL.west().above(), GantryPart.LEFT_HIGH), "the left leg is two tall");
        helper.assertTrue(isPart(helper, RAIL.east(), GantryPart.RIGHT_LOW), "the right leg stands east of the rail");
        helper.assertTrue(isPart(helper, RAIL.east().above(), GantryPart.RIGHT_HIGH), "the right leg is two tall");
        helper.assertTrue(isPart(helper, RAIL.above(), GantryPart.BEAM), "the beam is above the rail");
        helper.assertTrue(helper.getBlockState(RAIL).is(ServusShuttlesBlocks.RAIL.get()), "the rail stays");
        helper.assertTrue(left.isEmpty(), "placing used up the item");
        helper.succeed();
    }

    /** A curve takes no gantry. */
    @ServusGameTest
    public static void gantryRefusedOnCurve(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 4);
        WarehouseFixtures.stone(helper, RAIL.east(2).below());
        helper.setBlock(RAIL.east(), ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        GantryFixtures.shape(helper, RAIL.east());
        GantryFixtures.shape(helper, RAIL);
        ItemStack stack = GantryFixtures.use(helper, RAIL, new ItemStack(ServusShuttlesItems.FLUID_GANTRY.get()));
        helper.assertTrue(noGantryAround(helper, RAIL), "nothing is placed on a curve");
        helper.assertTrue(stack.getCount() == 1, "the player keeps the gantry");
        helper.succeed();
    }

    /** A block where the upper part of a leg would go stops the placement. */
    @ServusGameTest
    public static void gantryRefusedWithoutRoom(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 6);
        helper.setBlock(RAIL.east().above(), Blocks.STONE.defaultBlockState());
        ItemStack stack = GantryFixtures.use(helper, RAIL, new ItemStack(ServusShuttlesItems.FLUID_GANTRY.get()));
        helper.assertTrue(noGantryAround(helper, RAIL), "nothing is placed without room");
        helper.assertTrue(stack.getCount() == 1, "the player keeps the gantry");
        helper.succeed();
    }

    /** Breaking the beam takes the whole gantry and drops one item. */
    @ServusGameTest
    public static void breakingAnyPartRemovesAll(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 6);
        GantryFixtures.place(helper, RAIL);
        helper.getLevel().destroyBlock(helper.absolutePos(RAIL.above()), true);
        helper.assertTrue(noGantryAround(helper, RAIL), "no part is left");
        helper.assertTrue(GantryFixtures.droppedGantries(helper, RAIL).size() == 1, "one gantry item dropped");
        helper.succeed();
    }

    /** The dropped item keeps tanks, locks, mode and upgrades, and placing it brings them back. */
    @ServusGameTest
    public static void droppedGantryKeepsTanksLocksModeUpgrades(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 6);
        BlockPos other = new BlockPos(8, 1, 4);
        GantryFixtures.railAlongZ(helper, other.getX(), 2, 6);
        FluidGantryBlockEntity gantry = GantryFixtures.place(helper, RAIL);
        gantry.upgrades().setStackInSlot(0, new ItemStack(ServusFoundationItems.CAPACITY_UPGRADE.get()));
        gantry.upgrades().setStackInSlot(1, new ItemStack(ServusFoundationItems.CAPACITY_UPGRADE.get()));
        gantry.setLock(0, Fluids.WATER);
        gantry.setLock(1, Fluids.LAVA);
        gantry.setMode(GantryMode.EXPORT);
        gantry.tank(0).fill(new FluidStack(Fluids.WATER, 5000), false);
        helper.getLevel().destroyBlock(helper.absolutePos(RAIL.east().above()), true);
        List<ItemStack> drops = GantryFixtures.droppedGantries(helper, RAIL);
        helper.assertTrue(drops.size() == 1, "one gantry item dropped, got " + drops.size());
        GantryFixtures.use(helper, other, drops.getFirst().copy());
        FluidGantryBlockEntity placed = GantryFixtures.master(helper, other);
        helper.assertTrue(placed.tank(0).getFluid().is(Fluids.WATER) && placed.tank(0).getFluidAmount() == 5000,
                "the left tank holds 5000 mB of water, got " + placed.tank(0).getFluidAmount());
        helper.assertTrue(placed.tank(1).isEmpty(), "the right tank is empty");
        helper.assertTrue(placed.lock(0) == Fluids.WATER && placed.lock(1) == Fluids.LAVA, "the locks came along");
        helper.assertTrue(placed.mode() == GantryMode.EXPORT, "the mode came along");
        helper.assertTrue(placed.upgrades().count() == 2, "both upgrades came along");
        helper.assertTrue(placed.capacity() == 48000, "two upgrades give 48000 mB, got " + placed.capacity());
        helper.assertTrue(helper.getBlockState(other.above()).getValue(FluidGantryBlock.MODE) == GantryMode.EXPORT,
                "the beam shows the mode");
        helper.succeed();
    }

    private static void press(FluidGantryMenu menu, Player player, int tank, int step, int times) {
        for (int i = 0; i < times; i++) {
            menu.clickMenuButton(player, FluidGantryMenu.keepButton(tank, step));
        }
    }

    /** The keep buttons step a locked Export tank by 1 B and 10 B, between 0 and the tank's capacity. */
    @ServusGameTest
    public static void keepLevelSetAndClamped(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 6);
        FluidGantryBlockEntity gantry = GantryFixtures.place(helper, RAIL);
        gantry.setMode(GantryMode.EXPORT);
        gantry.setLock(0, Fluids.WATER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FluidGantryMenu menu = new FluidGantryMenu(1, player.getInventory(), gantry);
        press(menu, player, 0, 3, 1);
        press(menu, player, 0, 2, 2);
        press(menu, player, 0, 1, 4);
        helper.assertTrue(gantry.keep(0) == 8000 && gantry.keptLevel(0) == 8000, "the left tank keeps 8 B, got " + gantry.keep(0));
        helper.assertTrue(menu.keep(0) == 8000 && menu.keepApplies(0), "the menu shows the keep level");
        helper.assertTrue(gantry.keep(1) == 0, "the right tank keeps nothing");
        press(menu, player, 0, 3, 3);
        helper.assertTrue(gantry.keep(0) == gantry.capacity(), "the keep level stops at the capacity, got " + gantry.keep(0));
        press(menu, player, 0, 0, 3);
        helper.assertTrue(gantry.keep(0) == 0, "the keep level stops at 0, got " + gantry.keep(0));
        helper.succeed();
    }

    /** The dropped item keeps the keep levels, and placing it brings them back. */
    @ServusGameTest
    public static void keepLevelRidesTheDroppedItem(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 6);
        BlockPos other = new BlockPos(8, 1, 4);
        GantryFixtures.railAlongZ(helper, other.getX(), 2, 6);
        FluidGantryBlockEntity gantry = GantryFixtures.place(helper, RAIL);
        gantry.setMode(GantryMode.EXPORT);
        gantry.setLock(1, Fluids.LAVA);
        gantry.setKeep(1, 12000);
        helper.getLevel().destroyBlock(helper.absolutePos(RAIL.above()), true);
        List<ItemStack> drops = GantryFixtures.droppedGantries(helper, RAIL);
        helper.assertTrue(drops.size() == 1, "one gantry item dropped, got " + drops.size());
        GantryFixtures.use(helper, other, drops.getFirst().copy());
        FluidGantryBlockEntity placed = GantryFixtures.master(helper, other);
        helper.assertTrue(placed.keep(1) == 12000 && placed.keptLevel(1) == 12000, "the right tank keeps 12 B, got " + placed.keep(1));
        helper.assertTrue(placed.keep(0) == 0, "the left tank keeps nothing");
        helper.succeed();
    }

    /** A tank on Any and a gantry outside Export mode keep no level, and the buttons do nothing on Any. */
    @ServusGameTest
    public static void keepLevelIgnoredOnAny(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 6);
        FluidGantryBlockEntity gantry = GantryFixtures.place(helper, RAIL);
        gantry.setMode(GantryMode.EXPORT);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FluidGantryMenu menu = new FluidGantryMenu(1, player.getInventory(), gantry);
        press(menu, player, 0, 3, 1);
        helper.assertTrue(gantry.keep(0) == 0 && !menu.keepApplies(0), "a tank on Any takes no keep level");
        gantry.setLock(0, Fluids.WATER);
        press(menu, player, 0, 3, 1);
        gantry.setLock(0, null);
        helper.assertTrue(gantry.keep(0) == 10000 && gantry.keptLevel(0) == 0, "back on Any the keep level is kept but does not apply");
        gantry.setLock(0, Fluids.WATER);
        gantry.setMode(GantryMode.STORAGE);
        helper.assertTrue(gantry.keptLevel(0) == 0 && !menu.showsKeep(), "a Storage gantry keeps no level");
        helper.succeed();
    }
}
