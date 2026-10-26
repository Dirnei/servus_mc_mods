package io.github.dirnei.mc.servus.foundation.logistics.gametest;

import static io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps.duct;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.FluidFilter;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.menu.UtilityDuctMenu;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/** GameTests of the duct fluid filters and fluid priorities. Vanilla cauldrons and Fluid Tanks are the fixtures. */
public final class DuctFluidFilterGameTests {

    /** Ducts in a row along x with a block north of each. */
    private static final BlockPos DUCT_A = new BlockPos(2, 1, 6);
    private static final BlockPos DUCT_B = new BlockPos(3, 1, 6);
    private static final BlockPos DUCT_C = new BlockPos(4, 1, 6);
    private static final BlockPos DUCT_D = new BlockPos(5, 1, 6);
    private static final BlockPos DUCT_E = new BlockPos(6, 1, 6);

    private DuctFluidFilterGameTests() {
    }

    private static void waterCauldron(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
    }

    private static FluidView handler(GameTestHelper helper, BlockPos pos, Direction side) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), side));
    }

    private static FluidStack fluid(GameTestHelper helper, BlockPos pos) {
        FluidView handler = handler(helper, pos, null);
        return handler == null ? FluidStack.EMPTY : handler.getFluidInTank(0);
    }

    private static boolean holds(GameTestHelper helper, BlockPos pos, Fluid fluid, int amount) {
        FluidStack stack = fluid(helper, pos);
        return amount == 0 ? stack.isEmpty() : stack.is(fluid) && stack.getAmount() == amount;
    }

    private static String describe(GameTestHelper helper, BlockPos pos) {
        FluidStack stack = fluid(helper, pos);
        return stack.isEmpty() ? "nothing" : stack.getAmount() + " mB of " + stack.typeHolder().getRegisteredName();
    }

    /** Water and lava cauldrons pull in on one channel; the empty cauldron on a lava only side receives lava only. */
    @ServusGameTest(timeoutTicks = 100)
    public static void lavaOnlySinkGetsLava(GameTestHelper helper) {
        waterCauldron(helper, DUCT_A.north());
        helper.setBlock(DUCT_B.north(), Blocks.LAVA_CAULDRON.defaultBlockState());
        helper.setBlock(DUCT_C.north(), Blocks.CAULDRON.defaultBlockState());
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.FLUID);
        UtilityDuctBlockEntity b = duct(helper, DUCT_B, Line.FLUID);
        UtilityDuctBlockEntity c = duct(helper, DUCT_C, Line.FLUID);
        a.setMode(Line.FLUID, Direction.NORTH, DuctMode.IN);
        b.setMode(Line.FLUID, Direction.NORTH, DuctMode.IN);
        c.setFluidFilter(Direction.NORTH, 0, Fluids.LAVA);

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(holds(helper, DUCT_C.north(), Fluids.LAVA, 1000), "the lava only cauldron holds lava, got "
                    + describe(helper, DUCT_C.north()));
            helper.assertTrue(holds(helper, DUCT_A.north(), Fluids.WATER, 1000), "the water stays in its cauldron");
            helper.succeed();
        });
    }

    /** A block with water and lava tanks gives only the listed fluid, or its first fluid without a filter. */
    @ServusGameTest(timeoutTicks = 20)
    public static void filteredOfferFromSeveralTanks(GameTestHelper helper) {
        FluidStacksResourceHandler tanks = new FluidStacksResourceHandler(2, 4000);
        FluidView view = new FluidView(tanks);
        view.fill(new FluidStack(Fluids.WATER, 1000), false);
        view.fill(new FluidStack(Fluids.LAVA, 2000), false);
        FluidFilter lavaOnly = new FluidFilter();
        lavaOnly.set(4, BuiltInRegistries.FLUID.getKey(Fluids.LAVA).toString());
        FluidStack offer = FluidChannelNetwork.offerOf(view, lavaOnly);
        helper.assertTrue(offer.is(Fluids.LAVA) && offer.getAmount() == 2000, "the lava only side is offered 2000 mB of lava, got " + offer);
        helper.assertTrue(FluidChannelNetwork.offerOf(view, new FluidFilter()).is(Fluids.WATER), "without a filter the first fluid is offered");
        FluidFilter milkOnly = new FluidFilter();
        milkOnly.set(0, "minecraft:milk");
        helper.assertTrue(FluidChannelNetwork.offerOf(view, milkOnly).isEmpty(), "an unlisted block offers nothing");
        helper.assertTrue(view.getFluidInTank(0).getAmount() == 1000 && view.getFluidInTank(1).getAmount() == 2000, "offers drain nothing");
        helper.succeed();
    }

    /** One bucket of water goes to the priority 5 cauldron, although the priority 0 cauldron comes first on the run. */
    @ServusGameTest(timeoutTicks = 100)
    public static void higherPriorityFillsFirst(GameTestHelper helper) {
        waterCauldron(helper, DUCT_A.north());
        helper.setBlock(DUCT_C.north(), Blocks.CAULDRON.defaultBlockState());
        helper.setBlock(DUCT_D.north(), Blocks.CAULDRON.defaultBlockState());
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.FLUID);
        duct(helper, DUCT_B, Line.FLUID);
        duct(helper, DUCT_C, Line.FLUID);
        UtilityDuctBlockEntity d = duct(helper, DUCT_D, Line.FLUID);
        a.setMode(Line.FLUID, Direction.NORTH, DuctMode.IN);
        d.setFluidPriority(Direction.NORTH, 5);

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(holds(helper, DUCT_D.north(), Fluids.WATER, 1000), "the priority 5 cauldron holds the water, got "
                    + describe(helper, DUCT_D.north()));
            helper.assertTrue(holds(helper, DUCT_C.north(), Fluids.WATER, 0), "the priority 0 cauldron stays empty");
            helper.succeed();
        });
    }

    /** A tank gives 1000 mB to two tanks on fluid priority 3: each receives 500 mB. */
    @ServusGameTest(timeoutTicks = 100)
    public static void samePriorityShares(GameTestHelper helper) {
        for (BlockPos duct : new BlockPos[] {DUCT_A, DUCT_C, DUCT_E}) {
            helper.setBlock(duct.north(), ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        }
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.FLUID);
        duct(helper, DUCT_B, Line.FLUID);
        UtilityDuctBlockEntity c = duct(helper, DUCT_C, Line.FLUID);
        duct(helper, DUCT_D, Line.FLUID);
        UtilityDuctBlockEntity e = duct(helper, DUCT_E, Line.FLUID);
        a.setMode(Line.FLUID, Direction.NORTH, DuctMode.IN);
        c.setFluidPriority(Direction.NORTH, 3);
        e.setFluidPriority(Direction.NORTH, 3);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(handler(helper, DUCT_A.north(), Direction.UP).fill(new FluidStack(Fluids.WATER, 1000), false) == 1000,
                    "the source tank takes 1000 mB");
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(holds(helper, DUCT_C.north(), Fluids.WATER, 500) && holds(helper, DUCT_E.north(), Fluids.WATER, 500),
                        "each tank receives 500 mB, got " + describe(helper, DUCT_C.north()) + " and " + describe(helper, DUCT_E.north()));
                helper.succeed();
            });
        });
    }

    /** A pump pushing water into a lava only side is refused; lava passes on. */
    @ServusGameTest(timeoutTicks = 60)
    public static void pushedWaterRefusedByLavaOnlySide(GameTestHelper helper) {
        helper.setBlock(DUCT_C.north(), Blocks.CAULDRON.defaultBlockState());
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.FLUID);
        duct(helper, DUCT_B, Line.FLUID);
        duct(helper, DUCT_C, Line.FLUID);
        a.setFluidFilter(Direction.NORTH, 0, Fluids.LAVA);

        helper.runAfterDelay(2, () -> {
            FluidView side = handler(helper, DUCT_A, Direction.NORTH);
            helper.assertTrue(side.fill(new FluidStack(Fluids.WATER, 1000), false) == 0, "water is refused");
            helper.assertTrue(side.fill(new FluidStack(Fluids.LAVA, 1000), false) == 1000, "lava passes on");
            helper.assertTrue(holds(helper, DUCT_C.north(), Fluids.LAVA, 1000), "the cauldron holds the lava");
            helper.succeed();
        });
    }

    /** Fluid filters and fluid priorities survive a save and load, the item priority stays separate. */
    @ServusGameTest(timeoutTicks = 20)
    public static void fluidFiltersSurviveReload(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT_B, Line.ITEM, Line.FLUID);
        duct.setPriority(Direction.WEST, 5);
        duct.setFluidPriority(Direction.WEST, -2);
        duct.setFluidFilter(Direction.WEST, 2, Fluids.LAVA);
        CompoundTag saved = duct.saveWithoutMetadata(helper.getLevel().registryAccess());
        duct.setFluidPriority(Direction.WEST, 0);
        duct.setFluidFilter(Direction.WEST, 2, null);
        duct.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(duct.priority(Direction.WEST) == 5, "the item priority is 5");
        helper.assertTrue(duct.fluidPriority(Direction.WEST) == -2, "the fluid priority is -2");
        helper.assertTrue("minecraft:lava".equals(duct.fluidFilter(Direction.WEST).get(2)), "slot 2 lists lava");
        helper.assertTrue(duct.fluidFilterAllows(Direction.WEST, Fluids.LAVA) && !duct.fluidFilterAllows(Direction.WEST, Fluids.WATER),
                "the west side passes only lava");
        helper.succeed();
    }

    /**
     * The fluid filter payload sets a slot by fluid id without the player owning anything, and refuses unknown
     * ids, empty fluid, slots out of range and ducts without a Fluid Pipe. A bucket names its fluid and stays full.
     */
    @ServusGameTest(timeoutTicks = 20)
    public static void fluidFilterSetById(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT_B, Line.FLUID);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(DUCT_B).east()));
        player.containerMenu = new UtilityDuctMenu(1, player.getInventory(), duct, Direction.NORTH);

        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        Optional<Fluid> named = Transfer.fluidOf(bucket);
        helper.assertTrue(named.isPresent() && named.get() == Fluids.WATER, "a water bucket names water, got " + named);
        helper.assertTrue(bucket.is(Items.WATER_BUCKET) && bucket.getCount() == 1, "the bucket stays full");
        helper.assertTrue(Transfer.fluidOf(new ItemStack(Items.COBBLESTONE)).isEmpty(), "cobblestone names no fluid");

        helper.assertTrue(UtilityDuctMenu.setFluidFilterById(player, 0, Optional.of(BuiltInRegistries.FLUID.getKey(named.get()))),
                "the water id is taken");
        helper.assertTrue("minecraft:water".equals(duct.fluidFilter(Direction.NORTH).get(0)), "slot 0 lists water");
        helper.assertFalse(UtilityDuctMenu.setFluidFilterById(player, 1, Optional.of(BuiltInRegistries.FLUID.getKey(Fluids.EMPTY))),
                "the empty fluid is refused");
        helper.assertFalse(UtilityDuctMenu.setFluidFilterById(player, FluidFilter.SLOTS, Optional.of(BuiltInRegistries.FLUID.getKey(Fluids.LAVA))),
                "a slot out of range is refused");
        helper.assertTrue(UtilityDuctMenu.setFluidFilterById(player, 0, Optional.empty()), "the none entry is taken");
        helper.assertTrue(duct.fluidFilter(Direction.NORTH).isEmpty(), "the none entry clears the slot");

        UtilityDuctBlockEntity items = duct(helper, DUCT_B.south(), Line.ITEM);
        player.containerMenu = new UtilityDuctMenu(2, player.getInventory(), items, Direction.NORTH);
        helper.assertFalse(UtilityDuctMenu.setFluidFilterById(player, 0, Optional.of(BuiltInRegistries.FLUID.getKey(Fluids.LAVA))),
                "a duct without a Fluid Pipe takes no fluid filter");
        helper.succeed();
    }
}
