package io.github.dirnei.mc.servus.manufacturing.alchemy.structure;

import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Copper Duct, a part of two builds. The Calciner takes three, from the Hood across and down
 * into the Sulfur tank; the Athanor takes two per tank, one block up, beside it and over the tank.
 * While formed, {@link #PIECE} says which piece it is: 0 to 2 for the Calciner, {@link #ATHANOR_NEAR}
 * and {@link #ATHANOR_OVER} for the Athanor. The build lies to the model's -x, so a formed duct's
 * build is {@code FACING.getCounterClockWise()} from it.
 */
public class CopperDuctBlock extends Block implements Wrenchable {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FORMED = CalcinerPartBlock.FORMED;
    /** The Athanor's duct right beside it. */
    public static final int ATHANOR_NEAR = 3;
    /** The Athanor's duct over its tank, turning down into it. */
    public static final int ATHANOR_OVER = 4;
    /** The duct cell of {@link CalcinerLayout} counted from {@link CalcinerLayout#FIRST_DUCT}, or an Athanor piece. */
    public static final IntegerProperty PIECE = IntegerProperty.create("piece", 0, ATHANOR_OVER);

    /** The pipe of an Athanor duct, low in the block so it can be jumped onto, along x or along z. */
    private static final VoxelShape PIPE_X = Block.box(0, 0, 6, 16, 4, 10);
    private static final VoxelShape PIPE_Z = Block.box(6, 0, 0, 10, 4, 16);

    public CopperDuctBlock(Properties properties) {
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

    /** Whether {@code state} is a duct formed into a Calciner. */
    public static boolean calcinerPiece(BlockState state) {
        return state.getBlock() instanceof CopperDuctBlock && state.getValue(FORMED) && state.getValue(PIECE) < ATHANOR_NEAR;
    }

    /** Whether {@code state} is a duct formed into an Athanor. */
    public static boolean athanorPiece(BlockState state) {
        return state.getBlock() instanceof CopperDuctBlock && state.getValue(FORMED) && state.getValue(PIECE) >= ATHANOR_NEAR;
    }

    /** The cell of {@link CalcinerLayout} a duct formed into a Calciner stands in. */
    public static CalcinerLayout.Cell cell(BlockState state) {
        return CalcinerLayout.cell(CalcinerLayout.FIRST_DUCT + state.getValue(PIECE));
    }

    /** A duct of the Athanor's build, formed as {@code piece} with the Athanor toward {@code toAthanor}. */
    public static BlockState athanorState(BlockState state, int piece, Direction toAthanor) {
        return state.setValue(FORMED, true).setValue(PIECE, piece).setValue(FACING, toAthanor.getClockWise());
    }

    /** Where the Athanor of a duct formed into one stands: one block down and one or two back along the run. */
    private static BlockPos athanorPos(BlockState state, BlockPos pos) {
        Direction toAthanor = state.getValue(FACING).getCounterClockWise();
        return pos.below().relative(toAthanor, state.getValue(PIECE) == ATHANOR_OVER ? 2 : 1);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!athanorPiece(state)) {
            return Shapes.block();
        }
        return state.getValue(FACING).getCounterClockWise().getAxis() == Direction.Axis.X ? PIPE_X : PIPE_Z;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!athanorPiece(state)) {
            CalcinerStructure.neighbourChanged(level, state, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (athanorPiece(state)) {
            if (level.getBlockEntity(athanorPos(state, pos)) instanceof AthanorBlockEntity athanor) {
                athanor.checkHearth();
            }
        } else {
            CalcinerStructure.broken(level, state, pos);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(FORMED) ? PushReaction.BLOCK : null;
    }

    /** The carrot never turns a duct of a build. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        return InteractionResult.PASS;
    }
}
