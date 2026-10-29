package io.github.dirnei.mc.servus.shuttles.rack;

import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesTags;
import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.block.BlockEntityItems;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.DataPotatoItem;
import io.github.dirnei.mc.servus.foundation.item.PotatoInspectable;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.shuttles.menu.HeavyRackMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneLabelItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Heavy Duty Rack: a chest sized bay that opens a GUI. Bays with the same
 * facing and wood form sections of a few bays: a bay joins the section beside it when
 * it is placed and that section has room, and the join never moves after
 * that. A section shares one GUI; every bay still stores its own slots.
 */
public class HeavyRackBlock extends BaseEntityBlock implements Wrenchable, PotatoInspectable {

    public static final MapCodec<HeavyRackBlock> CODEC = simpleCodec(HeavyRackBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Joined to the bay on the counter clockwise side: no post there, shelves run through. */
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    /** Joined to the bay on the clockwise side: no post there, shelves run through. */
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    private static final int MAX_SECTION_WALK = 64;

    public HeavyRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT);
    }

    private static boolean sameRow(BlockState state, BlockState neighbour) {
        return neighbour.getBlock() instanceof HeavyRackBlock && neighbour.getValue(FACING) == state.getValue(FACING);
    }

    /** The wood of the rack at that position; oak when there is none. */
    private static Identifier woodAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof HeavyRackBlockEntity rack ? rack.wood() : HeavyRackBlockEntity.DEFAULT_WOOD;
    }

    /** Same row and the same wood: only such bays join. */
    private static boolean sameBay(BlockState state, Identifier wood, BlockGetter level, BlockPos pos) {
        return sameRow(state, level.getBlockState(pos)) && wood.equals(woodAt(level, pos));
    }

    /**
     * Bays in the section that starts at the neighbour in {@code direction}
     * and extends away from {@code pos}, following that section's joins.
     * Zero when no rack of the same facing and wood is there.
     */
    private static int sectionSize(BlockState state, Identifier wood, BlockGetter level, BlockPos pos, Direction direction) {
        BooleanProperty onward = direction == state.getValue(FACING).getCounterClockWise() ? LEFT : RIGHT;
        BlockPos cursor = pos.relative(direction);
        BlockState current = level.getBlockState(cursor);
        if (!sameBay(state, wood, level, cursor)) {
            return 0;
        }
        int size = 1;
        while (size < MAX_SECTION_WALK && current.getValue(onward)) {
            cursor = cursor.relative(direction);
            current = level.getBlockState(cursor);
            if (!sameBay(state, wood, level, cursor)) {
                break;
            }
            size++;
        }
        return size;
    }

    /**
     * The bays of the section the rack at {@code pos} belongs to, from left to
     * right as seen from the front. Two bays share a section only when both
     * carry the join towards each other. Empty when no rack stands there.
     */
    public static List<BlockPos> section(BlockGetter level, BlockPos pos) {
        BlockState start = level.getBlockState(pos);
        if (!(start.getBlock() instanceof HeavyRackBlock)) {
            return List.of();
        }
        Direction right = start.getValue(FACING).getClockWise();
        int[] offsets = RackSection.bays(
                offset -> joined(start, level.getBlockState(pos.relative(right, offset)), LEFT),
                offset -> joined(start, level.getBlockState(pos.relative(right, offset)), RIGHT),
                MAX_SECTION_WALK);
        List<BlockPos> bays = new ArrayList<>(offsets.length);
        for (int offset : offsets) {
            bays.add(pos.relative(right, offset));
        }
        return List.copyOf(bays);
    }

    private static boolean joined(BlockState start, BlockState state, BooleanProperty flag) {
        return sameRow(start, state) && state.getValue(flag);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockGetter level = context.getLevel();
        Direction facing = RackFacing.of(context, block -> block instanceof HeavyRackBlock);
        BlockState state = defaultBlockState().setValue(FACING, facing);
        Identifier wood = HeavyRackBlockEntity.woodOf(context.getItemInHand());
        int max = ShuttlesStatsManager.INSTANCE.rackBaysPerSection();
        int leftSize = sectionSize(state, wood, level, pos, facing.getCounterClockWise());
        int rightSize = sectionSize(state, wood, level, pos, facing.getClockWise());
        Boolean leftBelow = joinFromBelow(state, level, pos, facing.getCounterClockWise(), LEFT, RIGHT);
        Boolean rightBelow = joinFromBelow(state, level, pos, facing.getClockWise(), RIGHT, LEFT);
        boolean joinLeft = leftBelow != null ? leftBelow && leftSize > 0 : RackRow.joinLeft(leftSize, max);
        boolean joinRight = rightBelow != null ? rightBelow && rightSize > 0 : RackRow.joinRight(joinLeft, leftSize, rightSize, max);
        return state
                .setValue(LEFT, joinLeft)
                .setValue(RIGHT, joinRight);
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
        // Section links follow the join flags, which the graph cannot see change.
        for (BlockPos bay : List.of(pos, pos.relative(facing.getCounterClockWise()), pos.relative(facing.getClockWise()))) {
            if (level.getBlockEntity(bay) instanceof StorageNodeBlockEntity node) {
                node.refreshLinks();
            }
        }
        // A bay joining a docked section closes it to pipes; a fresh section may be open.
        refreshDock(level, section(level, pos));
    }

    private static void mirror(Level level, BlockPos neighbourPos, BooleanProperty flag, boolean joined, BlockState placed) {
        BlockState neighbour = level.getBlockState(neighbourPos);
        if (joined && sameRow(placed, neighbour) && !neighbour.getValue(flag)) {
            level.setBlock(neighbourPos, neighbour.setValue(flag, true), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Only ever clears side joins: a neighbour that is gone or turned away
     * takes its join with it. Woods never change in place, so this skips the
     * wood check, which would misread a rack still being placed as oak.
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Direction facing = state.getValue(FACING);
        boolean same = sameRow(state, neighborState);
        if (direction == facing.getCounterClockWise()) {
            return same ? state : state.setValue(LEFT, false);
        }
        if (direction == facing.getClockWise()) {
            return same ? state : state.setValue(RIGHT, false);
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
        return createTickerHelper(type, ServusShuttlesBlockEntities.HEAVY_RACK.get(), HeavyRackBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || stack.getItem() instanceof ZoneLabelItem
                || stack.getItem() instanceof DataPotatoItem || hitResult.getDirection() != state.getValue(FACING)) {
            // Only the front opens the rack; elsewhere the held item acts, which places blocks.
            return InteractionResult.PASS;
        }
        return ServusShuttlesTags.placesOnRack(stack) ? InteractionResult.PASS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public List<Component> potatoReport(Level level, BlockPos pos, BlockState state, Vec3 hit) {
        return RackReport.of(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (hitResult.getDirection() != state.getValue(FACING)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HeavyRackBlockEntity rack) {
            player.openMenu(rack, buf -> HeavyRackMenu.writeSection(buf, level, pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HeavyRackBlockEntity rack && rack.frontRailChanged()) {
            refreshDock(level, section(level, pos));
        }
    }

    /** Whether a warehouse rail (or farm rail) stands in front of the bay at that position. */
    public static boolean railInFront(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof HeavyRackBlock
                && level.getBlockState(pos.relative(state.getValue(FACING))).getBlock() instanceof RailBlock;
    }

    /**
     * The section's pipe access may have changed: every bay drops its cached
     * capability and tells its neighbours, so ducts recompute their arms and
     * hoppers ask again. Event driven, never polled; it does not loop, because
     * the bays' front rail flags do not change on the second pass.
     */
    public static void refreshDock(Level level, List<BlockPos> bays) {
        for (BlockPos bay : bays) {
            level.invalidateCapabilities(bay);
            level.updateNeighborsAt(bay, level.getBlockState(bay).getBlock());
        }
    }

    /** A bay that leaves splits or shrinks the sections beside it: both may open to pipes. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Direction facing = state.getValue(FACING);
        for (BlockPos side : List.of(pos.relative(facing.getCounterClockWise()), pos.relative(facing.getClockWise()))) {
            if (level.getBlockState(side).getBlock() instanceof HeavyRackBlock) {
                refreshDock(level, section(level, side));
            }
        }
    }

    /** Sneaking dismantles the bay into an item that keeps its stock; the carrot never turns a rack. */
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
                    BlockEntityItems.saveToItem(rack, drop, level.registryAccess());
                    rack.clear();
                }
                Block.popResource(level, pos, drop);
                level.removeBlock(pos, false);
            }
        }
        return dismantle ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
