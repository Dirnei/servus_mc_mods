package io.github.dirnei.mc.servus.storage.rail;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Floor rail that links storage blocks into one warehouse network. Rails
 * connect to each other and to elevators sideways; a side that touches a
 * stock holding storage block is a dock and renders its own piece.
 */
public class RailBlock extends ConnectableBlock {

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
    protected boolean connectsTo(LevelAccessor level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        if (!direction.getAxis().isHorizontal()) {
            return false;
        }
        if (neighborState.getBlock() instanceof RailBlock || neighborState.getBlock() instanceof ElevatorBlock) {
            return true;
        }
        return docksTo(level, neighborPos, direction);
    }

    /** Whether the neighbour is a storage block that holds stock and takes a rail on the side facing this rail. */
    private static boolean docksTo(LevelAccessor level, BlockPos neighborPos, Direction direction) {
        return level.getBlockEntity(neighborPos) instanceof StorageNodeBlockEntity node && node.holdsStock()
                && node.acceptsRail(Conversions.toSide(direction.getOpposite()));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(dock(direction), docksTo(level, pos.relative(direction), direction));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        state = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if (direction.getAxis().isHorizontal()) {
            state = state.setValue(dock(direction), docksTo(level, neighborPos, direction));
        }
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportRigidBlock(level, pos.below());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && !state.canSurvive(level, pos)) {
            dropResources(state, level, pos, level.getBlockEntity(pos));
            level.removeBlock(pos, false);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RailBlockEntity(pos, state);
    }
}
