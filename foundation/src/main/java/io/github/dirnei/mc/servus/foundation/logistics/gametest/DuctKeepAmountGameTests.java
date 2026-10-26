package io.github.dirnei.mc.servus.foundation.logistics.gametest;

import static io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps.duct;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.menu.UtilityDuctMenu;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** GameTests of keep and fill up to amounts on duct filter entries. */
public final class DuctKeepAmountGameTests {

    /** Ducts in a row along x with a block north of each. */
    private static final BlockPos DUCT_A = new BlockPos(2, 1, 6);
    private static final BlockPos DUCT_B = new BlockPos(3, 1, 6);
    private static final BlockPos DUCT_C = new BlockPos(4, 1, 6);
    private static final BlockPos CHEST_A = DUCT_A.north();
    private static final BlockPos CHEST_B = DUCT_B.north();
    private static final BlockPos CHEST_C = DUCT_C.north();
    private static final int FUEL_SLOT = 1;

    private DuctKeepAmountGameTests() {
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST.defaultBlockState());
        return helper.getBlockEntity(pos, ChestBlockEntity.class);
    }

    private static int count(GameTestHelper helper, BlockPos pos, Item item) {
        ChestBlockEntity chest = helper.getBlockEntity(pos, ChestBlockEntity.class);
        int total = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Chest A pulls in on duct A with {@code item} listed with a keep amount; chest B on duct B receives. */
    private static void keepRow(GameTestHelper helper, Item item, int keep) {
        chest(helper, CHEST_B);
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.ITEM);
        duct(helper, DUCT_B, Line.ITEM);
        a.setMode(Line.ITEM, Direction.NORTH, DuctMode.IN_PULL);
        a.filter(Direction.NORTH).setStackInSlot(0, new ItemStack(item));
        a.setItemAmount(Direction.NORTH, 0, keep);
    }

    /** 100 saplings in two stacks with "oak sapling, keep 64": 36 move and 64 stay, and no more leave. */
    @ServusGameTest(timeoutTicks = 300)
    public static void keepSaplings(GameTestHelper helper) {
        ChestBlockEntity source = chest(helper, CHEST_A);
        source.setItem(0, new ItemStack(Items.OAK_SAPLING, 64));
        source.setItem(5, new ItemStack(Items.OAK_SAPLING, 36));
        keepRow(helper, Items.OAK_SAPLING, 64);

        helper.runAfterDelay(160, () -> {
            helper.assertTrue(count(helper, CHEST_A, Items.OAK_SAPLING) == 64, "chest A keeps 64 saplings, holds "
                    + count(helper, CHEST_A, Items.OAK_SAPLING));
            helper.assertTrue(count(helper, CHEST_B, Items.OAK_SAPLING) == 36, "chest B receives 36, holds "
                    + count(helper, CHEST_B, Items.OAK_SAPLING));
            helper.succeed();
        });
    }

    /** Three stacks of 64 cobblestone with "cobblestone, keep 100": the keep counts all slots, 92 leave. */
    @ServusGameTest(timeoutTicks = 400)
    public static void keepCountsAllSlots(GameTestHelper helper) {
        ChestBlockEntity source = chest(helper, CHEST_A);
        for (int slot : new int[] {0, 4, 9}) {
            source.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        keepRow(helper, Items.COBBLESTONE, 100);

        helper.runAfterDelay(250, () -> {
            helper.assertTrue(count(helper, CHEST_A, Items.COBBLESTONE) == 100, "chest A keeps 100, holds "
                    + count(helper, CHEST_A, Items.COBBLESTONE));
            helper.assertTrue(count(helper, CHEST_B, Items.COBBLESTONE) == 92, "chest B receives 92, holds "
                    + count(helper, CHEST_B, Items.COBBLESTONE));
            helper.succeed();
        });
    }

    private static FluidView tankHandler(GameTestHelper helper, BlockPos pos) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), Direction.UP));
    }

    private static int water(GameTestHelper helper, BlockPos pos) {
        FluidStack stack = tankHandler(helper, pos).getFluidInTank(0);
        return stack.is(Fluids.WATER) ? stack.getAmount() : 0;
    }

    /** A tank of 12,000 mB with "water, keep 8,000 mB" gives 4,000 mB; refilled by 2,000 it gives just those. */
    @ServusGameTest(timeoutTicks = 100)
    public static void keepWaterInTank(GameTestHelper helper) {
        helper.setBlock(CHEST_A, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        helper.setBlock(CHEST_C, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.FLUID);
        duct(helper, DUCT_B, Line.FLUID);
        duct(helper, DUCT_C, Line.FLUID);
        a.setMode(Line.FLUID, Direction.NORTH, DuctMode.IN);
        a.setFluidFilter(Direction.NORTH, 0, Fluids.WATER);
        a.setFluidAmount(Direction.NORTH, 0, 8000);

        helper.runAfterDelay(2, () -> {
            tankHandler(helper, CHEST_A).fill(new FluidStack(Fluids.WATER, 12000), false);
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(water(helper, CHEST_A) == 8000 && water(helper, CHEST_C) == 4000,
                        "8,000 stay and 4,000 move, got " + water(helper, CHEST_A) + " and " + water(helper, CHEST_C));
                tankHandler(helper, CHEST_A).fill(new FluidStack(Fluids.WATER, 2000), false);
                helper.runAfterDelay(5, () -> {
                    helper.assertTrue(water(helper, CHEST_A) == 8000 && water(helper, CHEST_C) == 6000,
                            "only the refill moves, got " + water(helper, CHEST_A) + " and " + water(helper, CHEST_C));
                    helper.succeed();
                });
            });
        });
    }

    /** A sink tank with "water, up to 3,000 mB" stops at 3,000 mB while the source keeps the rest. */
    @ServusGameTest(timeoutTicks = 100)
    public static void fillTankUpTo(GameTestHelper helper) {
        helper.setBlock(CHEST_A, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        helper.setBlock(CHEST_C, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.FLUID);
        duct(helper, DUCT_B, Line.FLUID);
        UtilityDuctBlockEntity c = duct(helper, DUCT_C, Line.FLUID);
        a.setMode(Line.FLUID, Direction.NORTH, DuctMode.IN);
        c.setMode(Line.FLUID, Direction.NORTH, DuctMode.OUT);
        c.setFluidFilter(Direction.NORTH, 0, Fluids.WATER);
        c.setFluidAmount(Direction.NORTH, 0, 3000);

        helper.runAfterDelay(2, () -> {
            tankHandler(helper, CHEST_A).fill(new FluidStack(Fluids.WATER, 10000), false);
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(water(helper, CHEST_C) == 3000 && water(helper, CHEST_A) == 7000,
                        "the sink fills to 3,000, got " + water(helper, CHEST_C) + ", source " + water(helper, CHEST_A));
                helper.succeed();
            });
        });
    }

    /**
     * A furnace on priority 1 with "coal, up to 16" and a chest on priority 0 share 64 coal as 16 and 48;
     * when the furnace loses 3 coal and 10 more arrive, it takes 3 and the chest 7.
     */
    @ServusGameTest(timeoutTicks = 400)
    public static void furnaceUpToSixteen(GameTestHelper helper) {
        chest(helper, CHEST_A).setItem(0, new ItemStack(Items.COAL, 64));
        helper.setBlock(CHEST_B, Blocks.FURNACE.defaultBlockState());
        chest(helper, CHEST_C);
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.ITEM);
        UtilityDuctBlockEntity b = duct(helper, DUCT_B, Line.ITEM);
        duct(helper, DUCT_C, Line.ITEM);
        a.setMode(Line.ITEM, Direction.NORTH, DuctMode.IN_PULL);
        b.setMode(Line.ITEM, Direction.NORTH, DuctMode.OUT);
        b.setPriority(Direction.NORTH, 1);
        b.filter(Direction.NORTH).setStackInSlot(0, new ItemStack(Items.COAL));
        b.setItemAmount(Direction.NORTH, 0, 16);

        helper.runAfterDelay(140, () -> {
            FurnaceBlockEntity furnace = helper.getBlockEntity(CHEST_B, FurnaceBlockEntity.class);
            helper.assertTrue(furnace.getItem(FUEL_SLOT).getCount() == 16, "the furnace holds 16 coal, got " + furnace.getItem(FUEL_SLOT));
            helper.assertTrue(count(helper, CHEST_C, Items.COAL) == 48, "the chest gets the other 48, got " + count(helper, CHEST_C, Items.COAL));
            furnace.setItem(FUEL_SLOT, new ItemStack(Items.COAL, 13));
            helper.getBlockEntity(CHEST_A, ChestBlockEntity.class).setItem(0, new ItemStack(Items.COAL, 10));
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(furnace.getItem(FUEL_SLOT).getCount() == 16, "the furnace is refilled to 16, got " + furnace.getItem(FUEL_SLOT));
                helper.assertTrue(count(helper, CHEST_C, Items.COAL) == 55, "the chest gets 7 more, got " + count(helper, CHEST_C, Items.COAL));
                helper.succeed();
            });
        });
    }

    /** Item and fluid amounts survive a save and load; a cleared slot loses its amount, also when set again. */
    @ServusGameTest(timeoutTicks = 20)
    public static void amountsSurviveReload(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT_B, Line.ITEM, Line.FLUID);
        duct.filter(Direction.EAST).setStackInSlot(3, new ItemStack(Items.OAK_SAPLING));
        duct.setItemAmount(Direction.EAST, 3, 64);
        duct.setFluidFilter(Direction.EAST, 1, Fluids.WATER);
        duct.setFluidAmount(Direction.EAST, 1, 8000);
        CompoundTag saved = duct.saveWithoutMetadata(helper.getLevel().registryAccess());
        duct.setItemAmount(Direction.EAST, 3, 0);
        duct.setFluidAmount(Direction.EAST, 1, 0);
        duct.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(duct.itemAmount(Direction.EAST, 3) == 64, "the sapling amount is 64 again");
        helper.assertTrue(duct.itemAmountFor(Direction.EAST, new ItemStack(Items.OAK_SAPLING)) == 64, "the sapling entry counts 64");
        helper.assertTrue(duct.fluidAmountFor(Direction.EAST, Fluids.WATER) == 8000, "the water amount is 8,000 mB again");

        duct.filter(Direction.EAST).setStackInSlot(3, ItemStack.EMPTY);
        duct.filter(Direction.EAST).setStackInSlot(3, new ItemStack(Items.OAK_SAPLING));
        helper.assertTrue(duct.itemAmount(Direction.EAST, 3) == 0, "a cleared slot set again has no amount");
        duct.setFluidFilter(Direction.EAST, 1, Fluids.LAVA);
        helper.assertTrue(duct.fluidFilter(Direction.EAST).amount(1) == 0, "a fluid slot set to another fluid has no amount");
        helper.succeed();
    }

    /** The amount payload sets set slots only, keeps amounts in range, and 0 removes an amount. */
    @ServusGameTest(timeoutTicks = 20)
    public static void amountSetThroughTheMenu(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT_B, Line.ITEM, Line.FLUID);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(DUCT_B).east()));
        player.containerMenu = new UtilityDuctMenu(1, player.getInventory(), duct, Direction.NORTH);
        duct.filter(Direction.NORTH).setStackInSlot(0, new ItemStack(Items.COAL));
        duct.setFluidFilter(Direction.NORTH, 2, Fluids.WATER);

        helper.assertTrue(UtilityDuctMenu.setFilterAmount(player, Line.ITEM.ordinal(), 0, 16), "the coal slot takes 16");
        helper.assertTrue(duct.itemAmount(Direction.NORTH, 0) == 16, "the coal slot shows 16");
        helper.assertFalse(UtilityDuctMenu.setFilterAmount(player, Line.ITEM.ordinal(), 1, 16), "an empty slot takes no amount");
        helper.assertTrue(UtilityDuctMenu.setFilterAmount(player, Line.ITEM.ordinal(), 0, Integer.MAX_VALUE), "a huge amount is taken");
        helper.assertTrue(duct.itemAmount(Direction.NORTH, 0) == UtilityDuctBlockEntity.MAX_ITEM_AMOUNT, "and kept at the item maximum");
        helper.assertTrue(UtilityDuctMenu.setFilterAmount(player, Line.FLUID.ordinal(), 2, 8000), "the water slot takes 8,000 mB");
        helper.assertTrue(duct.fluidFilter(Direction.NORTH).amount(2) == 8000, "the water slot shows 8,000 mB");
        helper.assertTrue(UtilityDuctMenu.setFilterAmount(player, Line.ITEM.ordinal(), 0, 0), "0 is taken");
        helper.assertTrue(duct.itemAmount(Direction.NORTH, 0) == 0 && !duct.filter(Direction.NORTH).getStackInSlot(0).isEmpty(),
                "0 removes the amount and keeps the coal");
        helper.assertFalse(UtilityDuctMenu.setFilterAmount(player, Line.ENERGY.ordinal(), 0, 5), "the power line has no amounts");
        helper.succeed();
    }
}
