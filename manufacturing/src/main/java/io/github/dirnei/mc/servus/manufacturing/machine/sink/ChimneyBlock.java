package io.github.dirnei.mc.servus.manufacturing.machine.sink;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A brick Chimney block. Stacked straight on a top block of a formed Sink it
 * raises the Sink's void rate; the top block of a column shows the cap. Placing
 * or removing one tells the Sink at the bottom of its column to count again.
 */
public class ChimneyBlock extends Block {

    public static final MapCodec<ChimneyBlock> CODEC = simpleCodec(ChimneyBlock::new);
    /** True on the top block of a column, which shows the cap. */
    public static final BooleanProperty TOP = BooleanProperty.create("top");

    /** The slim shaft, ten pixels wide; the top block adds a cap twelve pixels wide. */
    private static final VoxelShape SHAFT = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape WITH_CAP = Shapes.or(Block.box(3, 0, 3, 13, 13, 13), Block.box(2, 13, 2, 14, 16, 14));

    public ChimneyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TOP, true));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TOP) ? WITH_CAP : SHAFT;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState above = context.getLevel().getBlockState(context.getClickedPos().above());
        return defaultBlockState().setValue(TOP, !(above.getBlock() instanceof ChimneyBlock));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP) {
            return state.setValue(TOP, !(neighborState.getBlock() instanceof ChimneyBlock));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock())) {
            notifySink(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!newState.is(state.getBlock())) {
            notifySink(level, pos);
        }
    }

    /** Walks down through the column below {@code pos}; a formed Sink at its bottom counts its chimneys again. */
    private static void notifySink(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int step = 0; step <= SinkBlockEntity.MAX_COLUMN; step++) {
            cursor.move(Direction.DOWN);
            BlockState below = level.getBlockState(cursor);
            if (below.getBlock() instanceof ChimneyBlock) {
                continue;
            }
            SinkBlockEntity sink = SinkBlock.controller(level, cursor, below);
            if (sink != null) {
                sink.recountChimney();
            }
            return;
        }
    }
}
