package io.github.dirnei.mc.servus.manufacturing.duct;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A duct that pulls from its neighbours into the network. */
public class SplitterDuctBlock extends ItemDuctBlock {

    public static final MapCodec<SplitterDuctBlock> CODEC = simpleCodec(SplitterDuctBlock::new);

    public SplitterDuctBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SplitterDuctBlockEntity(pos, state);
    }
}
