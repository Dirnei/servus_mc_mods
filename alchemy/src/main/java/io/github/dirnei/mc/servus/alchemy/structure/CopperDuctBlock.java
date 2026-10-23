package io.github.dirnei.mc.servus.alchemy.structure;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerStructure;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Copper Duct, a part of two builds. The Calciner takes two, an elbow from the Hood and a drop
 * into the left Stone Bricks; the Athanor takes one per tank on its own level, between it and the tank.
 * While formed, {@link #PIECE} says which piece it is: 0 (elbow) and 1 (drop) for the Calciner, {@link #ATHANOR}
 * for the Athanor. The build lies to the model's -x, so a formed duct's build is
 * {@code FACING.getCounterClockWise()} from it.
 */
public class CopperDuctBlock extends Block implements Wrenchable {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FORMED = CalcinerPartBlock.FORMED;
    /** The Athanor's duct, a straight pipe from the tower's flange into the side of its tank. */
    public static final int ATHANOR = 2;
    /** The duct cell of {@link CalcinerLayout} counted from {@link CalcinerLayout#FIRST_DUCT}, or the Athanor piece. */
    public static final IntegerProperty PIECE = IntegerProperty.create("piece", 0, ATHANOR);

    /** The pipe of an Athanor duct at the height of the tower's flange, along x or along z; its top can be jumped onto from the hearth. */
    private static final VoxelShape PIPE_X = Block.box(0, 6, 6, 16, 10, 10);
    private static final VoxelShape PIPE_Z = Block.box(6, 6, 0, 10, 10, 16);

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
        return state.getBlock() instanceof CopperDuctBlock && state.getValue(FORMED) && state.getValue(PIECE) < ATHANOR;
    }

    /** Whether {@code state} is a duct formed into an Athanor. */
    public static boolean athanorPiece(BlockState state) {
        return state.getBlock() instanceof CopperDuctBlock && state.getValue(FORMED) && state.getValue(PIECE) == ATHANOR;
    }

    /** The cell of {@link CalcinerLayout} a duct formed into a Calciner stands in. */
    public static CalcinerLayout.Cell cell(BlockState state) {
        return CalcinerLayout.cell(CalcinerLayout.FIRST_DUCT + state.getValue(PIECE));
    }

    /** A duct of the Athanor's build, formed with the Athanor toward {@code toAthanor}. */
    public static BlockState athanorState(BlockState state, Direction toAthanor) {
        return state.setValue(FORMED, true).setValue(PIECE, ATHANOR).setValue(FACING, toAthanor.getClockWise());
    }

    /** Where the Athanor of a duct formed into one stands: right beside it, toward the model's -x. */
    private static BlockPos athanorPos(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING).getCounterClockWise());
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
