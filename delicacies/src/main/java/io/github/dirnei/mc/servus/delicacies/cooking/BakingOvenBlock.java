package io.github.dirnei.mc.servus.delicacies.cooking;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Baking Oven, a small steel box with a window in its door; it bakes baking recipes and its window glows meanwhile. */
public class BakingOvenBlock extends VesselBlock {

    public static final MapCodec<BakingOvenBlock> CODEC = simpleCodec(BakingOvenBlock::new);
    /** The light a baking oven gives off through its window. */
    private static final int BAKING_LIGHT = 7;

    public BakingOvenBlock(Properties properties) {
        super(properties, VesselParts.ovenShape());
    }

    /** Light level of {@code state}: some while it bakes, none otherwise. */
    public static int lightLevel(BlockState state) {
        return state.getValue(COOKING) ? BAKING_LIGHT : 0;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected float topHeight() {
        return VesselParts.OVEN_BODY[4];
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BakingOvenBlockEntity(pos, state);
    }
}
