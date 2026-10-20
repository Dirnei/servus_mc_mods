package io.github.dirnei.mc.servus.delicacies.cooking;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Pan, dark iron with a wooden handle; it fries pan recipes. */
public class PanBlock extends VesselBlock {

    public static final MapCodec<PanBlock> CODEC = simpleCodec(PanBlock::new);

    public PanBlock(Properties properties) {
        super(properties, VesselParts.panShape());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected float topHeight() {
        return VesselParts.PAN_NORTH[4];
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PanBlockEntity(pos, state);
    }
}
