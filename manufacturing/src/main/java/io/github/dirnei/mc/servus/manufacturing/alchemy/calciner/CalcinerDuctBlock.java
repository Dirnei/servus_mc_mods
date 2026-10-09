package io.github.dirnei.mc.servus.manufacturing.alchemy.calciner;

import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;

/**
 * The copper Sulfur Duct of the Calciner, placed three times: from the Hood across and down
 * into the Sulfur tank. While formed, {@link #PIECE} says which of the three it is.
 */
public class CalcinerDuctBlock extends Block implements Wrenchable {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FORMED = CalcinerPartBlock.FORMED;
    /** The duct cell of {@link CalcinerLayout}, counted from {@link CalcinerLayout#FIRST_DUCT}. */
    public static final IntegerProperty PIECE = IntegerProperty.create("piece", 0, 2);

    public CalcinerDuctBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false).setValue(PIECE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, PIECE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** The cell of {@link CalcinerLayout} a formed duct stands in. */
    public static CalcinerLayout.Cell cell(BlockState state) {
        return CalcinerLayout.cell(CalcinerLayout.FIRST_DUCT + state.getValue(PIECE));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        CalcinerStructure.neighbourChanged(level, state, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            CalcinerStructure.broken(level, state, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(FORMED) ? PushReaction.BLOCK : null;
    }

    /** The carrot never turns a duct of the Calciner. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        return InteractionResult.PASS;
    }
}
