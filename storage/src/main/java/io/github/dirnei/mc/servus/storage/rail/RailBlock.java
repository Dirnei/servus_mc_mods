package io.github.dirnei.mc.servus.storage.rail;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Floor rail that links storage blocks into one warehouse network. Rails
 * connect to each other sideways only; the network itself joins anything
 * touching the rail.
 */
public class RailBlock extends ConnectableBlock {

    public static final MapCodec<RailBlock> CODEC = simpleCodec(RailBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public RailBlock(Properties properties) {
        super(properties, 3);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean connectsTo(LevelAccessor level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        if (!direction.getAxis().isHorizontal()) {
            return false;
        }
        if (neighborState.getBlock() instanceof RailBlock || neighborState.getBlock() instanceof ElevatorBlock) {
            return true;
        }
        return level.getBlockEntity(neighborPos) instanceof StorageNodeBlockEntity node && node.holdsStock();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportRigidBlock(level, pos.below());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RailBlockEntity(pos, state);
    }
}
