package io.github.dirnei.mc.servus.foundation.machine.pump;

import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The compact pump block shared by the Burner and the Electric Pump: a base
 * plate resting on the source below, a narrower body with the front, an outlet
 * on top, and a pipe stub on every open face toward a duct or a fluid handling
 * neighbour, so ducts and tanks meet it without a gap. A stub is as thick as
 * the duct arm it meets. The stubs are absolute directions; the shape is
 * symmetric, so it fits every facing.
 */
public abstract class PumpBlock extends MachineBlock {

    /** Stub size: 0 none, else the number of lines it meets. A pump takes water plus fuel or energy, never both, so at most two. */
    public static final int MAX_STUB = 2;
    /** Stub size toward a tank or any other fluid block. */
    public static final int PLAIN_STUB = 2;

    public static final IntegerProperty NORTH = IntegerProperty.create("north", 0, MAX_STUB);
    public static final IntegerProperty SOUTH = IntegerProperty.create("south", 0, MAX_STUB);
    public static final IntegerProperty EAST = IntegerProperty.create("east", 0, MAX_STUB);
    public static final IntegerProperty WEST = IntegerProperty.create("west", 0, MAX_STUB);
    public static final IntegerProperty UP = IntegerProperty.create("up", 0, MAX_STUB);

    /** The faces a stub can show on, with their properties. */
    public static final Map<Direction, IntegerProperty> STUBS = new EnumMap<>(Map.of(
            Direction.NORTH, NORTH, Direction.SOUTH, SOUTH, Direction.EAST, EAST, Direction.WEST, WEST, Direction.UP, UP));

    private static final VoxelShape BODY = Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15),
            Block.box(3, 2, 3, 13, 12, 13),
            Block.box(6, 12, 6, 10, 15, 10));

    private final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();

    protected PumpBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (IntegerProperty property : STUBS.values()) {
            state = state.setValue(property, 0);
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
        return state.getBlock() instanceof PumpBlock pump && pump.opensFace(state, face);
    }

    /** The faces water, fuel or energy pass through; the Burner Pump closes more of them. */
    protected boolean opensFace(BlockState state, Direction face) {
        return face != Direction.DOWN && face != state.getValue(FACING);
    }

    /** Half width in pixels of a stub of the given size, the same as a duct arm with that many lines. */
    public static int stubRadius(int size) {
        return 1 + size;
    }

    /** The box of a stub of the given size on a face, in pixels: from x, y, z to x, y, z. */
    public static int[] stubBox(Direction face, int size) {
        int lo = 8 - stubRadius(size);
        int hi = 8 + stubRadius(size);
        return switch (face) {
            case NORTH -> new int[] {lo, lo, 0, hi, hi, 3};
            case SOUTH -> new int[] {lo, lo, 13, hi, hi, 16};
            case WEST -> new int[] {0, lo, lo, 3, hi, hi};
            case EAST -> new int[] {13, lo, lo, 16, hi, hi};
            default -> new int[] {lo, 12, lo, hi, 16, hi};
        };
    }

    /**
     * The stub size on an open face: the number of lines of a duct whose arms
     * reach the pump, {@link #PLAIN_STUB} toward any other block that takes or
     * gives fluid, else 0.
     */
    private static int stubSize(BlockState state, Level level, BlockPos pos, Direction face) {
        if (!givesWater(state, face)) {
            return 0;
        }
        BlockPos neighbour = pos.relative(face);
        if (level.getBlockEntity(neighbour) instanceof UtilityDuctBlockEntity duct) {
            int lines = 0;
            for (Line line : Line.VALUES) {
                if (duct.hasArm(line, face.getOpposite())) {
                    lines++;
                }
            }
            return Math.min(lines, MAX_STUB);
        }
        return level.getCapability(Capabilities.Fluid.BLOCK, neighbour, face.getOpposite()) != null ? PLAIN_STUB : 0;
    }

    /** The state with every stub matching its neighbour. Server only: the client gets the result through block updates. */
    private static BlockState withStubs(BlockState state, Level level, BlockPos pos) {
        for (Map.Entry<Direction, IntegerProperty> stub : STUBS.entrySet()) {
            state = state.setValue(stub.getValue(), stubSize(state, level, pos, stub.getKey()));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return context.getLevel() instanceof ServerLevel server ? withStubs(state, server, context.getClickedPos()) : state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        IntegerProperty property = STUBS.get(direction);
        if (property != null && level instanceof ServerLevel server) {
            return state.setValue(property, stubSize(state, server, pos, direction));
        }
        return state;
    }

    /** The water below or a neighbour's lines may have changed: check both instead of polling. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level instanceof ServerLevel server) {
            if (level.getBlockEntity(pos) instanceof PumpMachine pump) {
                pump.pump().requestCheck();
            }
            BlockState updated = withStubs(state, server, pos);
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(state, this::shapeOf);
    }

    /** The body without stubs; the compact pump's is the same for every facing. */
    protected VoxelShape bodyShape(BlockState state) {
        return BODY;
    }

    private VoxelShape shapeOf(BlockState state) {
        VoxelShape shape = bodyShape(state);
        for (Map.Entry<Direction, IntegerProperty> stub : STUBS.entrySet()) {
            int size = state.getValue(stub.getValue());
            if (size > 0) {
                int[] box = stubBox(stub.getKey(), size);
                shape = Shapes.or(shape, Block.box(box[0], box[1], box[2], box[3], box[4], box[5]));
            }
        }
        return shape;
    }
}
