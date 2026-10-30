package io.github.dirnei.mc.servus.alchemy.venter;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A Dung Bed of the Venter Equinus: spruce and straw around the Cucurbit, where the Hay Bales are laid in. */
public class DungBedBlock extends VenterFrameBlock {

    private static final VoxelShape PLAIN_SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public DungBedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape plainShape(BlockState state) {
        return PLAIN_SHAPE;
    }
}
