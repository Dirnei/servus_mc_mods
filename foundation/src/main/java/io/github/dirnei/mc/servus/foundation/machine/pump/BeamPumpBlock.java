package io.github.dirnei.mc.servus.foundation.machine.pump;

import io.github.dirnei.mc.servus.foundation.machine.TwoHalves;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A beam engine pump two blocks tall, placed and broken as one through
 * {@link TwoHalves}: the Burner Pump and the Electric Pump. The lower half is
 * the machine (block entity, buffer, stubs) and opens only its back and sides;
 * the upper half carries the beam's pivot and forwards clicks to the lower
 * half. Each pump adds its own part shapes.
 */
public abstract class BeamPumpBlock extends PumpBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF = TwoHalves.HALF;

    protected BeamPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    /** Quarter turns from the north built models to {@code facing}, as the blockstate rotates them. */
    public static int quarters(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360 / 90;
    }

    /** A box in pixels as a shape. */
    protected static VoxelShape box(float[] box) {
        return Shapes.box(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0, box[4] / 16.0, box[5] / 16.0);
    }

    /** The union of the parts turned to {@code facing}. */
    protected static VoxelShape shapeOf(float[][] parts, Direction facing) {
        VoxelShape shape = Shapes.empty();
        for (float[] part : parts) {
            shape = Shapes.or(shape, box(BeamPumpParts.rotated(part, quarters(facing))));
        }
        return shape;
    }

    /** The lower half's body without stubs, for a facing. */
    protected abstract VoxelShape lowerShape(Direction facing);

    /** The upper half's parts that do not move, for a facing. */
    protected abstract VoxelShape upperShape(Direction facing);

    @Override
    protected VoxelShape bodyShape(BlockState state) {
        Direction facing = state.getValue(FACING);
        return isUpper(state) ? upperShape(facing) : lowerShape(facing);
    }

    public static boolean isUpper(BlockState state) {
        return TwoHalves.isUpper(state);
    }

    /** The position of the lower half, the one with the block entity. */
    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return TwoHalves.lowerPos(state, pos);
    }

    /** Only the lower half's back and sides: its top is the upper half, the upper half has no faces of its own. */
    @Override
    protected boolean opensFace(BlockState state, Direction face) {
        return !isUpper(state) && face != Direction.UP && super.opensFace(state, face);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return TwoHalves.canPlace(context) ? super.getStateForPlacement(context) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        TwoHalves.placeUpper(level, pos, upperOf(state));
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    /** The upper half matching a lower half: same facing, no stubs. */
    public BlockState upperOf(BlockState lower) {
        return defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER).setValue(FACING, lower.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        BlockState kept = TwoHalves.keepTogether(this, state, direction, neighborState);
        if (kept != null) {
            return kept.isAir() || !isUpper(kept) ? kept : kept.setValue(FACING, neighborState.getValue(FACING));
        }
        return isUpper(state) ? state : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        if (!isUpper(state)) {
            super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        TwoHalves.breakPartner(this, level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isUpper(state) ? null : super.newBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return super.useWithoutItem(state, level, lowerPos(state, pos), player, hitResult);
    }

    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        return TwoHalves.forwardWrench(context, state, super::onWrench);
    }
}
