package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.foundation.block.SideConfigWrench;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.DataPotatoItem;
import io.github.dirnei.mc.servus.foundation.item.PotatoInspectable;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSideHandler;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

/**
 * A fluid station two blocks high: the {@link DoubleBlockHalf#LOWER} half
 * keeps the block entity, the upper half forwards clicks, buckets, the
 * potato and the carrot to it. Either half going away takes the other with
 * it, and the lower half drops the item that keeps the tanks and sides.
 * Its front faces the player who placed it; the lamps are the lower half's.
 */
public abstract class FluidStationBlock extends BaseEntityBlock implements Wrenchable, PotatoInspectable {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    protected FluidStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(StorageNodeBlocks.ONLINE, false).setValue(StorageNodeBlocks.ACTIVITY, StorageNodeBlocks.Activity.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, StorageNodeBlocks.ONLINE, StorageNodeBlocks.ACTIVITY);
    }

    /** The block entity of the lower half at {@code pos}. */
    protected abstract FluidStationBlockEntity createStation(BlockPos pos, BlockState state);

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? createStation(pos, state) : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** The lower half's position of the station whose half {@code state} stands at {@code pos}. */
    public static BlockPos lowerPos(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    /** The station either half at {@code pos} belongs to, or null when there is none or it is not loaded. */
    public static @Nullable FluidStationBlockEntity station(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidStationBlock)) {
            return null;
        }
        BlockPos lower = lowerPos(pos, state);
        if (level instanceof Level world && !world.isLoaded(lower)) {
            return null;
        }
        return level.getBlockEntity(lower) instanceof FluidStationBlockEntity station ? station : null;
    }

    /**
     * What a pipe on a face of either half sees: the Fluids side that face
     * belongs to, null on the front and on the faces where the halves meet.
     */
    public static @Nullable FluidSideHandler sideHandler(Level level, BlockPos pos, BlockState state, @Nullable Direction face) {
        if (face == null || !(state.getBlock() instanceof FluidStationBlock)) {
            return null;
        }
        boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
        if (face == (lower ? Direction.UP : Direction.DOWN)) {
            return null;
        }
        FluidStationBlockEntity station = station(level, pos);
        return station == null ? null : FluidSideHandler.of(station, face);
    }

    /** Null without room for both halves, so a plain placement places nothing; {@link FluidStationItem} says why. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (FluidStationItem.refusal(context) != null) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /** A half whose other half is gone turns into air, so a station never stays half built. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean towardsOther = direction == (half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        if (towardsOther && !(neighborState.is(this) && neighborState.getValue(HALF) != half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** A creative player breaking either half gets no item. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            FluidStationBlockEntity station = station(level, pos);
            if (station != null) {
                station.dropNothing();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    /** A fluid container fills or empties the tanks by hand; the carrot and the potato pass to their own handling. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || stack.getItem() instanceof DataPotatoItem) {
            return InteractionResult.PASS;
        }
        if (Transfer.isFluidContainer(stack)) {
            FluidStationBlockEntity station = station(level, pos);
            if (!level.isClientSide() && station != null) {
                // ItemAccess keeps the container of a creative player, like a vanilla bucket.
                FluidUtil.interactWithFluidHandler(player, hand, pos, station.manualHandler());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            FluidStationBlockEntity station = station(level, pos);
            if (station != null) {
                player.openMenu(station, station::writeOpening);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public List<Component> potatoReport(Level level, BlockPos pos, BlockState state, Vec3 hit) {
        FluidStationBlockEntity station = station(level, pos);
        return station == null ? List.of() : FluidStationReport.of(station);
    }

    /** The carrot cycles the clicked side's Fluids mode; sneaking dismantles the station into its item. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        FluidStationBlockEntity station = station(level, context.getClickedPos());
        if (station == null) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                level.removeBlock(station.getBlockPos(), false);
            }
            return InteractionResult.SUCCESS;
        }
        return SideConfigWrench.use(context, this, station, () -> { });
    }
}
