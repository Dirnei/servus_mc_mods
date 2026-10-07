package io.github.dirnei.mc.servus.manufacturing.machine.pump;

import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The compact pump block shared by the Burner and the Electric Pump: a base
 * plate resting on the source below, a narrower body with the front, an outlet
 * on top, and a pipe stub on every face that gives water to a fluid handling
 * neighbour, so pipes and tanks meet it without a gap. The stubs are
 * absolute directions; the shape is symmetric, so it fits every facing.
 */
public abstract class PumpBlock extends MachineBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;

    /** The faces a stub can show on, with their properties. */
    public static final Map<Direction, BooleanProperty> STUBS = new EnumMap<>(Map.of(
            Direction.NORTH, NORTH, Direction.SOUTH, SOUTH, Direction.EAST, EAST, Direction.WEST, WEST, Direction.UP, UP));

    private static final VoxelShape BODY = Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15),
            Block.box(3, 2, 3, 13, 12, 13),
            Block.box(6, 12, 6, 10, 15, 10));
    private static final Map<Direction, VoxelShape> STUB_SHAPES = new EnumMap<>(Map.of(
            Direction.NORTH, Block.box(5, 5, 0, 11, 11, 3),
            Direction.SOUTH, Block.box(5, 5, 13, 11, 11, 16),
            Direction.WEST, Block.box(0, 5, 5, 3, 11, 11),
            Direction.EAST, Block.box(13, 5, 5, 16, 11, 11),
            Direction.UP, Block.box(6, 15, 6, 10, 16, 10)));

    private final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();

    protected PumpBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (BooleanProperty property : STUBS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, SOUTH, EAST, WEST, UP);
    }

    /** Whether the pump gives water through a face: every face but the front and the bottom. */
    public static boolean givesWater(BlockState state, Direction face) {
        return face != Direction.DOWN && face != state.getValue(FACING);
    }

    private static boolean connects(BlockState state, Level level, BlockPos pos, Direction face) {
        return givesWater(state, face)
                && level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(face), face.getOpposite()) != null;
    }

    /** The state with every stub matching its neighbour. Server only: the client gets the result through block updates. */
    private static BlockState withStubs(BlockState state, Level level, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> stub : STUBS.entrySet()) {
            state = state.setValue(stub.getValue(), connects(state, level, pos, stub.getKey()));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return context.getLevel() instanceof ServerLevel server ? withStubs(state, server, context.getClickedPos()) : state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        BooleanProperty property = STUBS.get(direction);
        if (property != null && level instanceof ServerLevel server) {
            return state.setValue(property, connects(state, server, pos, direction));
        }
        return state;
    }

    /** The water below or a neighbour's fluid handling may have changed: check both instead of polling. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        PumpBlocks.requestCheck(level, pos);
        if (level instanceof ServerLevel server) {
            BlockState updated = withStubs(state, server, pos);
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(state, PumpBlock::shapeOf);
    }

    private static VoxelShape shapeOf(BlockState state) {
        VoxelShape shape = BODY;
        for (Map.Entry<Direction, BooleanProperty> stub : STUBS.entrySet()) {
            if (state.getValue(stub.getValue())) {
                shape = Shapes.or(shape, STUB_SHAPES.get(stub.getKey()));
            }
        }
        return shape;
    }
}
