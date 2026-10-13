package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.fluids.FluidStack;

/** Places farm rails, Field Heads and crops in GameTests. Positions are relative to the test. */
final class FieldFixtures {

    private FieldFixtures() {
    }

    /** Stone under {@code pos}, then a rail of that block on it, shaped by its neighbours. */
    static void rail(GameTestHelper helper, BlockPos pos, Block rail) {
        WarehouseFixtures.stone(helper, pos.below());
        helper.setBlock(pos, Block.updateFromNeighbourShapes(rail.defaultBlockState(), helper.getLevel(), helper.absolutePos(pos)));
    }

    /** A farm rail on stone, the way a player places it. */
    static void farmRail(GameTestHelper helper, BlockPos pos) {
        rail(helper, pos, ServusStorageBlocks.FARM_RAIL.get());
    }

    static FieldHeadBlockEntity head(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        return helper.getBlockEntity(pos, FieldHeadBlockEntity.class);
    }

    /** A crop of that age on farmland. */
    static void crop(GameTestHelper helper, BlockPos pos, Block crop, int age) {
        helper.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(pos, ((CropBlock) crop).getStateForAge(age));
    }

    /** Whether the farmland at {@code pos} counts as near water. */
    static boolean wet(GameTestHelper helper, BlockPos pos) {
        return FarmlandWaterManager.hasBlockWaterTicket(helper.getLevel(), helper.absolutePos(pos));
    }

    /** The fluid handler of the Field Head at {@code pos}. */
    static FluidView fluid(GameTestHelper helper, BlockPos pos) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), Direction.UP));
    }

    /** Fills the head with 4000 mB of water. */
    static void water(FieldHeadBlockEntity head) {
        Transfer.fill(head.fluidHandler(), new FluidStack(Fluids.WATER, 4000), false);
    }
}
