package io.github.dirnei.mc.servus.foundation.machine.pump;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Burner Pump: a small beam engine two blocks tall, placed and broken as
 * one like a door. The lower half is the machine (block entity, fuel, buffer,
 * stubs); the upper half carries chimney, cylinder and beam pivot and forwards
 * clicks to the lower half.
 *
 * <p>Follows the 1.21.1 {@code DoorBlock}: {@code getStateForPlacement} returns
 * null without a replaceable block above, {@code setPlacedBy} sets the upper
 * half, {@code updateShape(state, direction, neighborState, level, pos,
 * neighborPos)} turns a half without its partner into air (which
 * {@code Block.updateOrDestroy} destroys with drops), and
 * {@code playerWillDestroy(level, pos, state, player)} removes the bottom
 * without drops in creative or without the right tool, as
 * {@code DoublePlantBlock.preventDropFromBottomPart} does.
 */
public class BurnerPumpBlock extends PumpBlock {

    public static final MapCodec<BurnerPumpBlock> CODEC = simpleCodec(BurnerPumpBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public BurnerPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return MachineContent.BURNER_PUMP_ENTITY.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    public static boolean isUpper(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    /** The position of the lower half, the one with the block entity. */
    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return isUpper(state) ? pos.below() : pos;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), upperOf(state), Block.UPDATE_ALL);
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    /** The upper half matching a lower half: same facing, no stubs. */
    public BlockState upperOf(BlockState lower) {
        return defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER).setValue(FACING, lower.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        boolean upper = isUpper(state);
        if (direction == (upper ? Direction.DOWN : Direction.UP)) {
            if (!neighborState.is(this) || isUpper(neighborState) == upper) {
                return Blocks.AIR.defaultBlockState();
            }
            return upper ? state.setValue(FACING, neighborState.getValue(FACING)) : state;
        }
        return upper ? state : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
                                   boolean movedByPiston) {
        if (!isUpper(state)) {
            super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && isUpper(state) && (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (lower.is(this) && !isUpper(lower)) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, below, Block.getId(lower));
            }
        }
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
        if (isUpper(state) && context.getPlayer() != null) {
            BlockPos below = context.getClickedPos().below();
            BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), below, context.isInside());
            return super.onWrench(new UseOnContext(context.getPlayer(), context.getHand(), hit), context.getLevel().getBlockState(below));
        }
        return super.onWrench(context, state);
    }
}
