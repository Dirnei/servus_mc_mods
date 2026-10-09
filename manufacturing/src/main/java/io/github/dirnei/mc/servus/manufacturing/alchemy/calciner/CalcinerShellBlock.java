package io.github.dirnei.mc.servus.manufacturing.alchemy.calciner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A brick of a formed Calciner. Forming puts it in place of vanilla Bricks so the brick can
 * carry the facing and its cell; unforming puts the Bricks back. It has no item of its own.
 */
public class CalcinerShellBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** The brick cell of {@link CalcinerLayout}, 1 to 6. */
    public static final IntegerProperty CELL = IntegerProperty.create("cell", 1, 6);

    public CalcinerShellBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CELL, 1));
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
        return PushReaction.BLOCK;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CELL);
    }
}
