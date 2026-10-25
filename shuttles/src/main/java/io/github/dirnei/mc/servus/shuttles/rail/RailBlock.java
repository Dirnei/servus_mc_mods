package io.github.dirnei.mc.servus.shuttles.rail;

import net.minecraft.world.level.ScheduledTickAccess;
import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import io.github.dirnei.mc.servus.foundation.item.DataPotatoItem;
import io.github.dirnei.mc.servus.foundation.item.PotatoInspectable;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleReport;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rail that links storage blocks into one warehouse network. Rails connect to
 * each other and to elevators sideways; a side that touches a stock holding
 * storage block is a dock and renders its own piece. A rail stands on a floor,
 * hangs on a rack front or elevator segment, or is free in the air; free rails
 * bridge short gaps and fall when the gap does not hold (see {@link RailSpan}).
 * The Data Potato reports the shuttles on it.
 */
public class RailBlock extends ConnectableBlock implements PotatoInspectable {

    public static final MapCodec<RailBlock> CODEC = simpleCodec(RailBlock::new);
    public static final BooleanProperty DOCK_NORTH = BooleanProperty.create("dock_north");
    public static final BooleanProperty DOCK_SOUTH = BooleanProperty.create("dock_south");
    public static final BooleanProperty DOCK_WEST = BooleanProperty.create("dock_west");
    public static final BooleanProperty DOCK_EAST = BooleanProperty.create("dock_east");
    private static final Map<Direction, BooleanProperty> DOCKS = Map.of(
            Direction.NORTH, DOCK_NORTH, Direction.SOUTH, DOCK_SOUTH,
            Direction.WEST, DOCK_WEST, Direction.EAST, DOCK_EAST);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public RailBlock(Properties properties) {
        super(properties, 3);
        registerDefaultState(defaultBlockState()
                .setValue(DOCK_NORTH, false).setValue(DOCK_SOUTH, false)
                .setValue(DOCK_WEST, false).setValue(DOCK_EAST, false));
    }

    /** The dock flag of a horizontal side. */
    public static BooleanProperty dock(Direction direction) {
        return DOCKS.get(direction);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DOCK_NORTH, DOCK_SOUTH, DOCK_WEST, DOCK_EAST);
    }

    @Override
    protected boolean connectsTo(LevelReader level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        if (!direction.getAxis().isHorizontal()) {
            return false;
        }
        if (neighborState.getBlock() instanceof RailBlock || neighborState.getBlock() instanceof ElevatorBlock) {
            return true;
        }
        return docksTo(level, neighborPos, direction)
                && level.getBlockEntity(neighborPos) instanceof StorageNodeBlockEntity node && node.bendsRail();
    }

    /** Whether the neighbour is a storage block, station or garage that takes a rail on the side facing this rail. */
    private static boolean docksTo(LevelReader level, BlockPos neighborPos, Direction direction) {
        return level.getBlockEntity(neighborPos) instanceof StorageNodeBlockEntity node && node.docksRail()
                && node.acceptsRail(Conversions.toSide(direction.getOpposite()));
    }

    /** Whether the rail draws a dock piece towards the neighbour: linked nodes that show one, not garages. */
    private static boolean showsDockTo(LevelReader level, BlockPos neighborPos, Direction direction) {
        return docksTo(level, neighborPos, direction)
                && level.getBlockEntity(neighborPos) instanceof StorageNodeBlockEntity node && node.showsDock();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(dock(direction), showsDockTo(level, pos.relative(direction), direction));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        state = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
        ticks.scheduleTick(pos, this, 1);
        if (direction.getAxis().isHorizontal()) {
            state = state.setValue(dock(direction), showsDockTo(level, neighborPos, direction));
        }
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Placed on a floor, or in the air beside a rail or an anchor; whether it stays is decided by {@link RailSpan}. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (canSupportRigidBlock(level, pos.below())) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            if (level.getBlockState(neighborPos).getBlock() instanceof RailBlock || isAnchor(level, neighborPos, direction)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the neighbour holds a rail up: an elevator segment, a heavy rack whose front faces the rail, or a standing rail. */
    private static boolean isAnchor(LevelReader level, BlockPos neighborPos, Direction direction) {
        Block block = level.getBlockState(neighborPos).getBlock();
        return block instanceof ElevatorBlock
                || block instanceof RailBlock && canSupportRigidBlock(level, neighborPos.below())
                || level.getBlockEntity(neighborPos) instanceof HeavyRackBlockEntity rack
                && rack.acceptsRail(Conversions.toSide(direction.getOpposite()));
    }

    /** Standing on a floor, hung on an anchor or free; only meaningful for a position that holds a rail. */
    static RailSpan.Kind kind(LevelReader level, BlockPos pos) {
        if (canSupportRigidBlock(level, pos.below())) {
            return RailSpan.Kind.STANDING;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isAnchor(level, pos.relative(direction), direction)) {
                return RailSpan.Kind.HUNG;
            }
        }
        return RailSpan.Kind.FREE;
    }

    private static List<BlockPos> linkedRails(LevelReader level, BlockPos pos) {
        List<BlockPos> rails = new ArrayList<>(4);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            if (level.getBlockState(neighborPos).getBlock() instanceof RailBlock) {
                rails.add(neighborPos);
            }
        }
        return rails;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock())) {
            level.scheduleTick(pos, this, 1);
        }
    }

    /**
     * Checks the gaps of this rail and the rails beside it a tick after something
     * changed and drops the ones that do not hold. The neighbours are checked too
     * because their kind depends on this rail: a rail that loses its floor but
     * stays hung turns the rail beside it free without notifying it.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        List<BlockPos> rails = linkedRails(level, pos);
        rails.add(0, pos);
        for (BlockPos start : rails) {
            if (!(level.getBlockState(start).getBlock() instanceof RailBlock)) {
                continue;
            }
            RailSpan.Verdict<BlockPos> verdict = RailSpan.check(start, rail -> kind(level, rail),
                    rail -> linkedRails(level, rail), ShuttlesStatsManager.INSTANCE.railMaxSpan());
            for (BlockPos rail : verdict.drop()) {
                level.destroyBlock(rail, true);
            }
        }
    }

    /** The Data Potato gets the click, for its report. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (stack.getItem() instanceof DataPotatoItem) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public List<Component> potatoReport(Level level, BlockPos pos, BlockState state, Vec3 hit) {
        return ShuttleReport.of(level, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RailBlockEntity(pos, state);
    }
}
