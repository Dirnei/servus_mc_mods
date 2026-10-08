package io.github.dirnei.mc.servus.storage.rail;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One segment of a rail elevator shaft: four corner posts around an open
 * middle. Segments stack vertically into a shaft with one lift car and take
 * rails on their four sides; the network joins anything touching them.
 * Entities pass through and climb the shaft like a ladder.
 */
public class ElevatorBlock extends ConnectableBlock {

    public static final MapCodec<ElevatorBlock> CODEC = simpleCodec(ElevatorBlock::new);

    public ElevatorBlock(Properties properties) {
        super(properties, 4);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean connectsTo(LevelAccessor level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        if (direction.getAxis().isVertical()) {
            return neighborState.getBlock() instanceof ElevatorBlock;
        }
        return neighborState.getBlock() instanceof RailBlock;
    }

    /** The whole block is the outline, so a segment is easy to target, like scaffolding. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /** Players and entities walk through the shaft; the climbable tag lets them climb it. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** A segment stands on a solid block or on another segment. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).getBlock() instanceof ElevatorBlock || canSupportRigidBlock(level, below);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && !state.canSurvive(level, pos)) {
            dropResources(state, level, pos, level.getBlockEntity(pos));
            level.removeBlock(pos, false);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElevatorBlockEntity(pos, state);
    }
}
