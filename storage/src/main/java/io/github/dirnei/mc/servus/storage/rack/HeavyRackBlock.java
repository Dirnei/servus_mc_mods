package io.github.dirnei.mc.servus.storage.rack;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.lib.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.lib.item.Wrenchable;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Heavy Duty Rack: a chest sized bay that opens a GUI and joins neighbouring
 * bays with the same facing into one continuous rack with a post every few
 * bays. The joins are visual only; every bay keeps its own inventory.
 */
public class HeavyRackBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<HeavyRackBlock> CODEC = simpleCodec(HeavyRackBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** No post on the counter clockwise side; shelves and beams run through to that bay. */
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    /** No post on the clockwise side; shelves and beams run through to that bay. */
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final int MAX_ROW_WALK = 256;

    public HeavyRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LEFT, false).setValue(RIGHT, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return withJoins(state, context.getLevel(), context.getClickedPos());
    }

    private static boolean sameRow(BlockState state, BlockState neighbour) {
        return neighbour.getBlock() instanceof HeavyRackBlock && neighbour.getValue(FACING) == state.getValue(FACING);
    }

    /** Same facing racks in a row from this position in the given direction, this one excluded. */
    private static int countAlong(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        int count = 0;
        BlockPos cursor = pos.relative(direction);
        while (count < MAX_ROW_WALK && sameRow(state, level.getBlockState(cursor))) {
            count++;
            cursor = cursor.relative(direction);
        }
        return count;
    }

    /** Recomputes all four join flags from the row and the stack. */
    private static BlockState withJoins(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        int leftCount = countAlong(state, level, pos, facing.getCounterClockWise());
        int rightCount = countAlong(state, level, pos, facing.getClockWise());
        int every = StorageStatsManager.INSTANCE.rackPostsEvery();
        int bayCount = leftCount + 1 + rightCount;
        return state
                .setValue(LEFT, RackRow.leftPostHidden(leftCount, every))
                .setValue(RIGHT, RackRow.rightPostHidden(leftCount, bayCount, every))
                .setValue(UP, sameRow(state, level.getBlockState(pos.above())))
                .setValue(DOWN, sameRow(state, level.getBlockState(pos.below())));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return withJoins(state, level, pos);
    }

    /** Re-evaluates every bay of the row this position belongs to, in both directions. */
    private static void refreshRow(Level level, BlockPos pos, Direction facing) {
        for (Direction direction : new Direction[] {facing.getCounterClockWise(), facing.getClockWise()}) {
            BlockPos cursor = pos.relative(direction);
            int walked = 0;
            while (walked++ < MAX_ROW_WALK) {
                BlockState state = level.getBlockState(cursor);
                if (!(state.getBlock() instanceof HeavyRackBlock) || state.getValue(FACING) != facing) {
                    break;
                }
                BlockState fresh = withJoins(state, level, cursor);
                if (fresh != state) {
                    level.setBlock(cursor, fresh, Block.UPDATE_CLIENTS);
                }
                cursor = cursor.relative(direction);
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            refreshRow(level, pos, state.getValue(FACING));
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeavyRackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ServusStorageBlockEntities.HEAVY_RACK.get(), HeavyRackBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HeavyRackBlockEntity rack) {
            player.openMenu(rack, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        StorageNodeBlockEntity.neighborsChanged(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof HeavyRackBlockEntity rack) {
            ItemStackHandler items = rack.items();
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack stack = items.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                    items.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            refreshRow(level, pos, state.getValue(FACING));
        }
    }

    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean dismantle = player != null && player.isShiftKeyDown();
        if (!level.isClientSide()) {
            if (dismantle) {
                ItemStack drop = new ItemStack(this);
                if (level.getBlockEntity(pos) instanceof HeavyRackBlockEntity rack) {
                    rack.saveToItem(drop, level.registryAccess());
                    rack.clear();
                }
                Block.popResource(level, pos, drop);
                level.removeBlock(pos, false);
            } else {
                Direction oldFacing = state.getValue(FACING);
                BlockState rotated = state.setValue(FACING, oldFacing.getClockWise());
                level.setBlock(pos, withJoins(rotated, level, pos), Block.UPDATE_ALL);
                if (level.getBlockEntity(pos) instanceof HeavyRackBlockEntity rack) {
                    rack.facingChanged();
                }
                refreshRow(level, pos, oldFacing);
                refreshRow(level, pos, oldFacing.getClockWise());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
