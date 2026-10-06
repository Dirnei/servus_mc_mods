package io.github.dirnei.mc.servus.storage.rack;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.lib.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.lib.item.Wrenchable;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.zone.ZoneLabelItem;
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
 * Heavy Duty Rack: a chest sized bay that opens a GUI. Bays with the same
 * facing form sections of a few bays: a bay joins the section beside it when
 * it is placed and that section has room, and the join never moves after
 * that. Joins are visual only; every bay keeps its own inventory.
 */
public class HeavyRackBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<HeavyRackBlock> CODEC = simpleCodec(HeavyRackBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** Joined to the bay on the counter clockwise side: no post there, shelves run through. */
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    /** Joined to the bay on the clockwise side: no post there, shelves run through. */
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final int MAX_SECTION_WALK = 64;

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

    private static boolean sameRow(BlockState state, BlockState neighbour) {
        return neighbour.getBlock() instanceof HeavyRackBlock && neighbour.getValue(FACING) == state.getValue(FACING);
    }

    /**
     * Bays in the section that starts at the neighbour in {@code direction}
     * and extends away from {@code pos}, following that section's joins.
     * Zero when no same facing rack is there.
     */
    private static int sectionSize(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        BooleanProperty onward = direction == state.getValue(FACING).getCounterClockWise() ? LEFT : RIGHT;
        BlockPos cursor = pos.relative(direction);
        BlockState current = level.getBlockState(cursor);
        if (!sameRow(state, current)) {
            return 0;
        }
        int size = 1;
        while (size < MAX_SECTION_WALK && current.getValue(onward)) {
            cursor = cursor.relative(direction);
            current = level.getBlockState(cursor);
            if (!sameRow(state, current)) {
                break;
            }
            size++;
        }
        return size;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockGetter level = context.getLevel();
        BlockState below = level.getBlockState(pos.below());
        Direction facing = below.getBlock() instanceof HeavyRackBlock
                ? below.getValue(FACING)
                : context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        int max = StorageStatsManager.INSTANCE.rackBaysPerSection();
        int leftSize = sectionSize(state, level, pos, facing.getCounterClockWise());
        int rightSize = sectionSize(state, level, pos, facing.getClockWise());
        Boolean leftBelow = joinFromBelow(state, level, pos, facing.getCounterClockWise(), LEFT, RIGHT);
        Boolean rightBelow = joinFromBelow(state, level, pos, facing.getClockWise(), RIGHT, LEFT);
        boolean joinLeft = leftBelow != null ? leftBelow && leftSize > 0 : RackRow.joinLeft(leftSize, max);
        boolean joinRight = rightBelow != null ? rightBelow && rightSize > 0 : RackRow.joinRight(joinLeft, leftSize, rightSize, max);
        return state
                .setValue(LEFT, joinLeft)
                .setValue(RIGHT, joinRight)
                .setValue(UP, sameRow(state, level.getBlockState(pos.above())))
                .setValue(DOWN, sameRow(state, level.getBlockState(pos.below())));
    }

    /**
     * The row below decides where sections split: the rack under this bay, or
     * the rack under the neighbour on that side, says whether the join exists.
     * Null when no rack stands under either.
     */
    private static Boolean joinFromBelow(BlockState state, BlockGetter level, BlockPos pos, Direction side,
                                         BooleanProperty ownFlag, BooleanProperty neighbourFlag) {
        BlockState below = level.getBlockState(pos.below());
        if (sameRow(state, below)) {
            return below.getValue(ownFlag);
        }
        BlockState belowNeighbour = level.getBlockState(pos.relative(side).below());
        if (sameRow(state, belowNeighbour)) {
            return belowNeighbour.getValue(neighbourFlag);
        }
        return null;
    }

    /** Mirrors a fresh bay's joins onto its neighbours, since a join is mutual. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide() || oldState.is(this)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        mirror(level, pos.relative(facing.getCounterClockWise()), RIGHT, state.getValue(LEFT), state);
        mirror(level, pos.relative(facing.getClockWise()), LEFT, state.getValue(RIGHT), state);
    }

    private static void mirror(Level level, BlockPos neighbourPos, BooleanProperty flag, boolean joined, BlockState placed) {
        BlockState neighbour = level.getBlockState(neighbourPos);
        if (joined && sameRow(placed, neighbour) && !neighbour.getValue(flag)) {
            level.setBlock(neighbourPos, neighbour.setValue(flag, true), Block.UPDATE_CLIENTS);
        }
    }

    /** Only ever clears joins: a neighbour that is gone or turned away takes its join with it. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        boolean same = sameRow(state, neighborState);
        if (direction == facing.getCounterClockWise()) {
            return same ? state : state.setValue(LEFT, false);
        }
        if (direction == facing.getClockWise()) {
            return same ? state : state.setValue(RIGHT, false);
        }
        if (direction == Direction.UP) {
            return state.setValue(UP, same);
        }
        if (direction == Direction.DOWN) {
            return state.setValue(DOWN, same);
        }
        return state;
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
        if (stack.getItem() instanceof ConfigurationCarrotItem || stack.getItem() instanceof ZoneLabelItem
                || hitResult.getDirection() != state.getValue(FACING)) {
            // Only the front opens the rack; elsewhere the held item acts, which places blocks.
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (hitResult.getDirection() != state.getValue(FACING)) {
            return InteractionResult.PASS;
        }
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
            }
        }
        // The carrot never turns a rack: its facing is a layout decision made at placement.
        return dismantle ? InteractionResult.sidedSuccess(level.isClientSide()) : InteractionResult.PASS;
    }
}
