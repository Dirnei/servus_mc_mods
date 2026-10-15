package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.PrincipleDropperBlockEntity;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** The Principle Dropper: filled from outside, it drips what an unripe vein drinks onto the Gnome Hollow below and nowhere else. */
public final class DropperGameTests {

    private static final BlockPos BELOW = AlchemyGameTests.MACHINE;
    private static final BlockPos DROPPER = BELOW.above();
    /** Long enough for several drips at the shipped interval of 20 ticks. */
    private static final int SEVERAL_DRIPS = 70;

    private DropperGameTests() {
    }

    private static PrincipleDropperBlockEntity dropper(GameTestHelper helper) {
        helper.setBlock(DROPPER, ServusAlchemyBlocks.PRINCIPLE_DROPPER.get().defaultBlockState());
        return helper.getBlockEntity(DROPPER, PrincipleDropperBlockEntity.class);
    }

    private static FluidView capability(GameTestHelper helper, BlockPos pos, Direction side) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), side));
    }

    /** Fills the Dropper through its fluid capability, as a pipe would. */
    private static void fill(GameTestHelper helper, Principle principle, int amount) {
        FluidView view = capability(helper, DROPPER, Direction.NORTH);
        helper.assertTrue(view != null, "the Dropper should expose a fluid handler");
        int filled = view.fill(new FluidStack(PrincipleFluids.fluid(principle), amount), false);
        helper.assertTrue(filled == amount, "the Dropper should take " + amount + " mB " + principle + ", took " + filled);
    }

    /** Salt and Mercury piped into the Dropper drip into an unripe iron Hollow below and ripen its vein. */
    @ServusGameTest(timeoutTicks = 400)
    public static void dropperFedByCapabilityRipensHollow(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = HollowGameTests.ironHollow(helper, false);
        PrincipleDropperBlockEntity dropper = dropper(helper);
        fill(helper, Principle.SALT, 1000);
        fill(helper, Principle.MERCURY, 1000);
        helper.succeedWhen(() -> {
            helper.assertTrue(hollow.growth().ripeness >= 1, "the vein should grow from dripped principles, ripeness " + hollow.growth().ripeness);
            helper.assertTrue(dropper.tanks().amount(Principle.SALT) < 1000, "the Dropper's Salt should go down");
        });
    }

    /** The Dropper takes only principles, each into its own tank, and gives nothing back. */
    @ServusGameTest(timeoutTicks = 20)
    public static void dropperTakesOnlyPrinciplesAndNeverGives(GameTestHelper helper) {
        PrincipleDropperBlockEntity dropper = dropper(helper);
        FluidView view = capability(helper, DROPPER, Direction.UP);
        helper.assertTrue(view.fill(new FluidStack(Fluids.WATER, 500), false) == 0, "water is refused");
        fill(helper, Principle.SALT, 500);
        helper.assertTrue(dropper.tanks().amount(Principle.SALT) == 500, "the Salt tank holds 500 mB");
        helper.assertTrue(view.drain(100, false).isEmpty(), "nothing drains out");
        helper.assertTrue(view.drain(new FluidStack(PrincipleFluids.fluid(Principle.SALT), 100), false).isEmpty(), "no Salt drains out");
        helper.assertTrue(dropper.tanks().amount(Principle.SALT) == 500, "the Salt stays");
        helper.succeed();
    }

    /** On plain stone the Dropper keeps what it holds. */
    @ServusGameTest(timeoutTicks = SEVERAL_DRIPS + 20)
    public static void nothingWithoutAHollowBelow(GameTestHelper helper) {
        helper.setBlock(BELOW, Blocks.STONE);
        PrincipleDropperBlockEntity dropper = dropper(helper);
        fill(helper, Principle.SALT, 500);
        helper.runAfterDelay(SEVERAL_DRIPS, () -> {
            helper.assertTrue(dropper.tanks().amount(Principle.SALT) == 500, "the Salt should stay, got " + dropper.tanks().amount(Principle.SALT));
            helper.succeed();
        });
    }

    /** A Fluid Tank below gets nothing: the Dropper pushes into no block. */
    @ServusGameTest(timeoutTicks = SEVERAL_DRIPS + 20)
    public static void doesNotPushIntoATank(GameTestHelper helper) {
        helper.setBlock(BELOW, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        PrincipleDropperBlockEntity dropper = dropper(helper);
        fill(helper, Principle.SALT, 500);
        helper.runAfterDelay(SEVERAL_DRIPS, () -> {
            helper.assertTrue(capability(helper, BELOW, Direction.UP).getFluidInTank(0).isEmpty(), "the tank below should stay empty");
            helper.assertTrue(dropper.tanks().amount(Principle.SALT) == 500, "the Dropper keeps its Salt");
            helper.succeed();
        });
    }

    /** A Hollow without a vein, and one whose vein is ripe, get nothing. */
    @ServusGameTest(timeoutTicks = 2 * SEVERAL_DRIPS + 20)
    public static void ripeOrEmptyHollowGetsNothing(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        HollowGameTests.finishVillach(helper.getLevel(), team);
        GnomeHollowBlockEntity hollow = HollowGameTests.hollow(helper, team);
        PrincipleDropperBlockEntity dropper = dropper(helper);
        fill(helper, Principle.SALT, 500);
        helper.runAfterDelay(SEVERAL_DRIPS, () -> {
            helper.assertTrue(dropper.tanks().amount(Principle.SALT) == 500, "a Hollow without a vein gets no Salt");
            hollow.seed(new ItemStack(Items.RAW_IRON));
            hollow.growth().ripeness = hollow.veinDef().orElseThrow().growth().steps();
            helper.runAfterDelay(SEVERAL_DRIPS, () -> {
                helper.assertTrue(dropper.tanks().amount(Principle.SALT) == 500, "a ripe vein gets no Salt");
                helper.assertTrue(hollow.tanks().amount(Principle.SALT) == 0, "the ripe Hollow's tank stays empty");
                helper.succeed();
            });
        });
    }

    /** Sulfur, which an iron vein's steps do not take, stays in the Dropper. */
    @ServusGameTest(timeoutTicks = SEVERAL_DRIPS + 20)
    public static void onlyWhatTheVeinTakes(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = HollowGameTests.ironHollow(helper, false);
        PrincipleDropperBlockEntity dropper = dropper(helper);
        fill(helper, Principle.SULFUR, 500);
        fill(helper, Principle.SALT, 500);
        helper.runAfterDelay(SEVERAL_DRIPS, () -> {
            helper.assertTrue(dropper.tanks().amount(Principle.SULFUR) == 500, "the Sulfur should stay in the Dropper");
            helper.assertTrue(hollow.tanks().amount(Principle.SULFUR) == 0, "no Sulfur reaches the Hollow");
            helper.assertTrue(hollow.tanks().amount(Principle.SALT) > 0, "Salt reaches the Hollow");
            helper.succeed();
        });
    }

    /** With a Dropper on top the Hollow still offers no fluid or item handler on any side. */
    @ServusGameTest(timeoutTicks = 20)
    public static void hollowStillHasNoCapability(GameTestHelper helper) {
        HollowGameTests.ironHollow(helper, false);
        dropper(helper);
        BlockPos pos = helper.absolutePos(BELOW);
        for (Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, pos, side) == null, "no fluids toward " + side);
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.Item.BLOCK, pos, side) == null, "no items toward " + side);
        }
        helper.succeed();
    }

    /** A Principle Vat and the three principles make a Principle Dropper on a Citrinitas Athanor, not on an Albedo one. */
    @ServusGameTest(timeoutTicks = 600)
    public static void dropperNeedsCitrinitas(GameTestHelper helper) {
        AthanorBlockEntity athanor = AlchemyGameTests.athanor(helper);
        AlchemyGameTests.saltRing(helper);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 1, "the salt ring makes Albedo, got " + athanor.tier());
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(ServusAlchemyItems.VAT.get()));
        helper.runAfterDelay(2, () -> AlchemyGameTests.pour(helper, new Principles(250, 250, 250)));
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).isEmpty(), "no Dropper at Albedo");
            AlchemyGameTests.amethystRing(helper);
            athanor.checkTier();
            helper.assertTrue(athanor.tier() == 2, "the amethyst ring makes Citrinitas, got " + athanor.tier());
            helper.succeedWhen(() -> {
                helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(ServusAlchemyItems.PRINCIPLE_DROPPER.get()),
                        "a Principle Dropper should be made");
                helper.assertTrue(athanor.principles().isEmpty(), "the principles are burned");
            });
        });
    }
}
