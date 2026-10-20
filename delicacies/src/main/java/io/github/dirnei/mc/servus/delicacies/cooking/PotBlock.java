package io.github.dirnei.mc.servus.delicacies.cooking;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Pot, family steel with two handles; it holds water and cooks pot recipes. */
public class PotBlock extends VesselBlock {

    public static final MapCodec<PotBlock> CODEC = simpleCodec(PotBlock::new);

    public PotBlock(Properties properties) {
        super(properties, VesselParts.potShape());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected float topHeight() {
        return VesselParts.POT_NORTH[4];
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PotBlockEntity(pos, state);
    }
}
