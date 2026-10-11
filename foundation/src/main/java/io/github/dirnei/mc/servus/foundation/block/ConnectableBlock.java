package io.github.dirnei.mc.servus.foundation.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thin block with six connection flags, used by cables and ducts. Subclasses
 * decide what a side connects to. Sneak wrench dismantles it.
 */
public abstract class ConnectableBlock extends BaseEntityBlock implements Wrenchable {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private final VoxelShape center;
    private final Map<Direction, VoxelShape> arms;
    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    /**
     * @param radius half width of the core in pixels (3 gives a 6 by 6 core)
     */
    protected ConnectableBlock(Properties properties, int radius) {
        super(properties);
        int lo = 8 - radius;
        int hi = 8 + radius;
        this.center = Block.box(lo, lo, lo, hi, hi, hi);
        this.arms = Map.of(
                Direction.NORTH, Block.box(lo, lo, 0, hi, hi, lo),
                Direction.SOUTH, Block.box(lo, lo, hi, hi, hi, 16),
                Direction.WEST, Block.box(0, lo, lo, lo, hi, hi),
                Direction.EAST, Block.box(hi, lo, lo, 16, hi, hi),
                Direction.DOWN, Block.box(lo, 0, lo, hi, lo, hi),
                Direction.UP, Block.box(lo, hi, lo, hi, 16, hi));
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(EAST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    public static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    /** Whether this block connects to the neighbour in that direction. */
    protected abstract boolean connectsTo(LevelReader level, BlockPos neighborPos, BlockState neighborState, Direction direction);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, WEST, EAST, UP, DOWN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            state = state.setValue(property(direction), connectsTo(level, neighborPos, level.getBlockState(neighborPos), direction));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.setValue(property(direction), connectsTo(level, neighborPos, neighborState, direction));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(state, s -> {
            VoxelShape shape = center;
            for (Direction direction : Direction.values()) {
                if (s.getValue(property(direction))) {
                    shape = Shapes.or(shape, arms.get(direction));
                }
            }
            return shape;
        });
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem) {
            return InteractionResult.PASS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide()) {
            // Wood blocks (rails) keep their wood on the dismantled item.
            Block.popResource(level, pos, Wood.stamp(new ItemStack(this), level.getBlockEntity(pos)));
            level.removeBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }
}
