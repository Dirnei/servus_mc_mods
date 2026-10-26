package io.github.dirnei.mc.servus.foundation.logistics.gametest;

import static io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps.duct;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** GameTests of the Item Pipe filling partial stacks before empty slots. */
public final class DuctStackingGameTests {

    private static final BlockPos DUCT_A = new BlockPos(2, 1, 6);
    private static final BlockPos DUCT_B = new BlockPos(3, 1, 6);
    private static final BlockPos SOURCE = DUCT_A.north();
    private static final BlockPos TARGET = DUCT_B.north();
    private static final int PARTIAL_SLOT = 2;

    private DuctStackingGameTests() {
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST.defaultBlockState());
        return helper.getBlockEntity(pos, ChestBlockEntity.class);
    }

    /** A source chest with 8 cobblestone pulled on duct A into a target chest on duct B whose third slot holds {@code partial}. */
    private static ChestBlockEntity pipeInto(GameTestHelper helper, int partial) {
        chest(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, 8));
        ChestBlockEntity target = chest(helper, TARGET);
        target.setItem(PARTIAL_SLOT, new ItemStack(Items.COBBLESTONE, partial));
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.ITEM);
        duct(helper, DUCT_B, Line.ITEM);
        a.setMode(Line.ITEM, Direction.NORTH, DuctMode.IN_PULL);
        return target;
    }

    /** 8 cobblestone into a chest whose first slot is empty and whose third holds 20: the third holds 28. */
    @ServusGameTest(timeoutTicks = 100)
    public static void partialStackFirst(GameTestHelper helper) {
        ChestBlockEntity target = pipeInto(helper, 20);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(target.getItem(PARTIAL_SLOT).getCount() == 28, "the third slot holds 28, has " + target.getItem(PARTIAL_SLOT));
            helper.assertTrue(target.getItem(0).isEmpty(), "the first slot stays empty, has " + target.getItem(0));
            helper.succeed();
        });
    }

    /** With 60 in the third slot, 4 of the 8 fill it and 4 go into the first empty slot. */
    @ServusGameTest(timeoutTicks = 100)
    public static void partialStackFull(GameTestHelper helper) {
        ChestBlockEntity target = pipeInto(helper, 60);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(target.getItem(PARTIAL_SLOT).getCount() == 64, "the third slot is full, has " + target.getItem(PARTIAL_SLOT));
            helper.assertTrue(target.getItem(0).is(Items.COBBLESTONE) && target.getItem(0).getCount() == 4,
                    "4 go into the first slot, has " + target.getItem(0));
            helper.succeed();
        });
    }
}
