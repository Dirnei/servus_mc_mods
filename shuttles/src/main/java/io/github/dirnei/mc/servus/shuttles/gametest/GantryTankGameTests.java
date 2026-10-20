package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/** The tanks of a Fluid Gantry: modes and pipes, buckets, locks and Capacity Upgrades. */
public final class GantryTankGameTests {

    private static final BlockPos RAIL = new BlockPos(4, 1, 4);

    private GantryTankGameTests() {
    }

    private static FluidGantryBlockEntity gantry(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 6);
        return GantryFixtures.place(helper, RAIL);
    }

    private static List<BlockPos> legs() {
        return List.of(RAIL.west(), RAIL.west().above(), RAIL.east(), RAIL.east().above());
    }

    private static ResourceHandler<FluidResource> handlerAt(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), side);
    }

    /** Uses the item in the main hand on the beam, as a right click there does; returns what the hand holds after. */
    private static ItemStack useOnBeam(GameTestHelper helper, Player player) {
        BlockPos beam = helper.absolutePos(RAIL.above());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(beam), Direction.NORTH, beam, false);
        helper.getLevel().getBlockState(beam).useItemOn(player.getItemInHand(InteractionHand.MAIN_HAND), helper.getLevel(), player,
                InteractionHand.MAIN_HAND, hit);
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static Player holding(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    /** A Storage gantry shows pipes no fluid handler on any leg face, and none on the beam. */
    @ServusGameTest
    public static void storageGantryHasNoFluidHandler(GameTestHelper helper) {
        gantry(helper);
        for (BlockPos leg : legs()) {
            for (Direction side : Direction.values()) {
                helper.assertTrue(handlerAt(helper, leg, side) == null, "no handler at " + leg + " " + side);
            }
        }
        helper.assertTrue(handlerAt(helper, RAIL.above(), Direction.UP) == null, "the beam takes no pipes");
        helper.succeed();
    }

    /** An Import gantry fills its locked tank from a pipe and gives nothing back. */
    @ServusGameTest
    public static void importGantryFilledByPipeGivesNothingBack(GameTestHelper helper) {
        FluidGantryBlockEntity gantry = gantry(helper);
        gantry.setLock(1, Fluids.WATER);
        gantry.setMode(GantryMode.IMPORT);
        ResourceHandler<FluidResource> pipe = handlerAt(helper, RAIL.west().above(), Direction.WEST);
        helper.assertTrue(pipe != null, "an Import gantry has a handler on its legs");
        int filled = Transfer.fill(pipe, new FluidStack(Fluids.WATER, 4000), false);
        helper.assertTrue(filled == 4000, "the pipe put in 4000 mB, took " + filled);
        helper.assertTrue(gantry.tank(1).getFluidAmount() == 4000 && gantry.tank(0).isEmpty(), "the water went to the tank locked to it");
        helper.assertTrue(Transfer.drain(pipe, 1000, false).isEmpty(), "an Import gantry gives nothing back");
        helper.assertTrue(gantry.tank(1).getFluidAmount() == 4000, "the water stays");
        helper.succeed();
    }

    /** An Export gantry is drained by a pipe and takes nothing in. */
    @ServusGameTest
    public static void exportGantryDrainedByPipeTakesNothing(GameTestHelper helper) {
        FluidGantryBlockEntity gantry = gantry(helper);
        gantry.tank(0).fill(new FluidStack(Fluids.WATER, 3000), false);
        gantry.setMode(GantryMode.EXPORT);
        ResourceHandler<FluidResource> pipe = handlerAt(helper, RAIL.east(), Direction.EAST);
        helper.assertTrue(pipe != null, "an Export gantry has a handler on its legs");
        FluidStack drained = Transfer.drain(pipe, 1000, false);
        helper.assertTrue(drained.is(Fluids.WATER) && drained.getAmount() == 1000, "the pipe drained 1000 mB of water");
        helper.assertTrue(Transfer.fill(pipe, new FluidStack(Fluids.WATER, 1000), false) == 0, "an Export gantry takes nothing");
        helper.assertTrue(gantry.tank(0).getFluidAmount() == 2000, "2000 mB are left, got " + gantry.tank(0).getFluidAmount());
        helper.succeed();
    }

    /** A water bucket empties into the tank locked to water, in Storage mode too. */
    @ServusGameTest
    public static void bucketFillsStorageGantryByLock(GameTestHelper helper) {
        FluidGantryBlockEntity gantry = gantry(helper);
        gantry.setLock(0, Fluids.WATER);
        gantry.tank(1).fill(new FluidStack(Fluids.WATER, 500), false);
        ItemStack after = useOnBeam(helper, holding(helper, new ItemStack(Items.WATER_BUCKET)));
        helper.assertTrue(gantry.tank(0).getFluidAmount() == 1000, "the left tank holds 1000 mB, got " + gantry.tank(0).getFluidAmount());
        helper.assertTrue(gantry.tank(1).getFluidAmount() == 500, "the right tank is unchanged");
        helper.assertTrue(after.is(Items.BUCKET), "the player holds an empty bucket");
        helper.succeed();
    }

    /** A lava bucket finds no tank when both are locked to water. */
    @ServusGameTest
    public static void bucketRefusedByLock(GameTestHelper helper) {
        FluidGantryBlockEntity gantry = gantry(helper);
        gantry.setLock(0, Fluids.WATER);
        gantry.setLock(1, Fluids.WATER);
        ItemStack after = useOnBeam(helper, holding(helper, new ItemStack(Items.LAVA_BUCKET)));
        helper.assertTrue(gantry.tank(0).isEmpty() && gantry.tank(1).isEmpty(), "nothing went in");
        helper.assertTrue(after.is(Items.LAVA_BUCKET), "the player keeps the lava bucket");
        helper.succeed();
    }

    /** An empty bucket fills from a tank. */
    @ServusGameTest
    public static void emptyBucketFillsFromGantry(GameTestHelper helper) {
        FluidGantryBlockEntity gantry = gantry(helper);
        gantry.tank(1).fill(new FluidStack(Fluids.WATER, 2000), false);
        ItemStack after = useOnBeam(helper, holding(helper, new ItemStack(Items.BUCKET)));
        helper.assertTrue(after.is(Items.WATER_BUCKET), "the player holds a water bucket");
        helper.assertTrue(gantry.tank(1).getFluidAmount() == 1000, "1000 mB are left, got " + gantry.tank(1).getFluidAmount());
        helper.succeed();
    }

    /** Two Capacity Upgrades give both tanks 48000 mB. */
    @ServusGameTest
    public static void capacityUpgradeGrowsBothTanks(GameTestHelper helper) {
        FluidGantryBlockEntity gantry = gantry(helper);
        helper.assertTrue(gantry.tank(0).getCapacity() == 16000, "16000 mB without upgrades");
        gantry.upgrades().setStackInSlot(0, new ItemStack(ServusFoundationItems.CAPACITY_UPGRADE.get()));
        gantry.upgrades().setStackInSlot(3, new ItemStack(ServusFoundationItems.CAPACITY_UPGRADE.get()));
        helper.assertTrue(gantry.tank(0).getCapacity() == 48000 && gantry.tank(1).getCapacity() == 48000,
                "both tanks hold 48000 mB, got " + gantry.tank(0).getCapacity() + " and " + gantry.tank(1).getCapacity());
        int filled = gantry.tank(1).fill(new FluidStack(Fluids.WATER, 50000), false);
        helper.assertTrue(filled == 48000, "the right tank takes 48000 mB, took " + filled);
        helper.succeed();
    }

    /** An upgrade cannot come out while a tank holds more than the capacity without it. */
    @ServusGameTest
    public static void upgradeRemovalRefusedWhenFull(GameTestHelper helper) {
        FluidGantryBlockEntity gantry = gantry(helper);
        gantry.upgrades().setStackInSlot(0, new ItemStack(ServusFoundationItems.CAPACITY_UPGRADE.get()));
        gantry.tank(0).fill(new FluidStack(Fluids.WATER, 20000), false);
        helper.assertTrue(!gantry.canRemoveUpgrade(), "20000 mB would not fit into 16000");
        gantry.tank(0).drain(4000, false);
        helper.assertTrue(gantry.canRemoveUpgrade(), "16000 mB fit exactly");
        helper.succeed();
    }

    /** A tank holding water takes no lava; the other tank on Any takes it. */
    @ServusGameTest
    public static void lavaRefusedByWaterTank(GameTestHelper helper) {
        FluidGantryBlockEntity gantry = gantry(helper);
        gantry.tank(0).fill(new FluidStack(Fluids.WATER, 1000), false);
        helper.assertTrue(gantry.tank(0).fill(new FluidStack(Fluids.LAVA, 1000), false) == 0, "the water tank takes no lava");
        gantry.setLock(1, Fluids.WATER);
        helper.assertTrue(Transfer.fill(gantry.manualHandler(), new FluidStack(Fluids.LAVA, 1000), false) == 0,
                "no tank takes lava: one holds water, the other is locked to water");
        helper.succeed();
    }

    /** A duct beside the upper right leg draws its arm to the gantry once the gantry turns Import, and loses it in Storage. */
    @ServusGameTest(timeoutTicks = 60)
    public static void ductArmFollowsTheMode(GameTestHelper helper) {
        FluidGantryBlockEntity gantry = gantry(helper);
        BlockPos duct = RAIL.east(2).above();
        GameTestSteps.duct(helper, duct, Line.FLUID);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockProperty(duct, ConnectableBlock.WEST, false);
            gantry.setMode(GantryMode.IMPORT);
            helper.runAfterDelay(2, () -> {
                helper.assertBlockProperty(duct, ConnectableBlock.WEST, true);
                gantry.setMode(GantryMode.STORAGE);
                helper.runAfterDelay(2, () -> {
                    helper.assertBlockProperty(duct, ConnectableBlock.WEST, false);
                    helper.succeed();
                });
            });
        });
    }
}
