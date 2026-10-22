package io.github.dirnei.mc.servus.foundation.machine.sink;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A stone Chimney block. Stacked straight on a top block of a formed Sink it
 * raises the Sink's void rate; the top block of a column shows the cap. Placing
 * or removing one tells the Sink at the bottom of its column to count again.
 * A column that does not stand on a formed Sink smokes from its top block by
 * its own settings: {@link #SMOKE} and {@link #REDSTONE}, set with the carrot
 * for the whole column. {@link #POWERED} and {@link #ON_SINK} hold the same
 * value on every block of a column, so the top block decides its smoke alone.
 */
public class ChimneyBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<ChimneyBlock> CODEC = simpleCodec(ChimneyBlock::new);
    /** True on the top block of a column, which shows the cap. */
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final EnumProperty<ChimneySmoke> SMOKE = EnumProperty.create("smoke", ChimneySmoke.class);
    public static final EnumProperty<ChimneyRedstone> REDSTONE = EnumProperty.create("redstone", ChimneyRedstone.class);
    /** True when any block of the column receives a redstone signal. */
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    /** True when the column stands on a formed Sink, which then controls the smoke. */
    public static final BooleanProperty ON_SINK = BooleanProperty.create("on_sink");

    /** The slim shaft, ten pixels wide; the top block adds a cap twelve pixels wide. */
    private static final VoxelShape SHAFT = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape WITH_CAP = Shapes.or(Block.box(3, 0, 3, 13, 13, 13), Block.box(2, 13, 2, 14, 16, 14));

    public ChimneyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TOP, true).setValue(SMOKE, ChimneySmoke.OFF)
                .setValue(REDSTONE, ChimneyRedstone.ALWAYS).setValue(POWERED, false).setValue(ON_SINK, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, SMOKE, REDSTONE, POWERED, ON_SINK);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChimneyBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TOP) ? WITH_CAP : SHAFT;
    }

    /** Whether the top block of this state's column smokes. */
    public static boolean smokes(BlockState state) {
        return ChimneyRules.smokes(state.getValue(SMOKE).rule(), state.getValue(REDSTONE).rule(),
                state.getValue(POWERED), state.getValue(ON_SINK));
    }

    /** A block placed on a Chimney copies its settings; any other starts at off and always. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState().setValue(TOP, !(level.getBlockState(pos.above()).getBlock() instanceof ChimneyBlock));
        BlockState below = level.getBlockState(pos.below());
        if (below.getBlock() instanceof ChimneyBlock) {
            return state.setValue(SMOKE, below.getValue(SMOKE)).setValue(REDSTONE, below.getValue(REDSTONE))
                    .setValue(POWERED, below.getValue(POWERED)).setValue(ON_SINK, below.getValue(ON_SINK));
        }
        return state.setValue(ON_SINK, SinkBlock.isFormed(below));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.UP) {
            return state.setValue(TOP, !(neighborState.getBlock() instanceof ChimneyBlock));
        }
        if (direction == Direction.DOWN) {
            return state.setValue(ON_SINK, onSinkFrom(neighborState));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** Whether a block standing on {@code below} stands on a formed Sink. */
    private static boolean onSinkFrom(BlockState below) {
        return below.getBlock() instanceof ChimneyBlock ? below.getValue(ON_SINK) : SinkBlock.isFormed(below);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (oldState.is(state.getBlock()) || level.isClientSide()) {
            return;
        }
        boolean onSink = onSinkFrom(level.getBlockState(pos.below()));
        if (state.getValue(ON_SINK) != onSink) {
            level.setBlock(pos, state.setValue(ON_SINK, onSink), Block.UPDATE_ALL);
        }
        notifySink(level, pos);
        updatePower(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        notifySink(level, pos);
        for (BlockPos half : List.of(pos.above(), pos.below())) {
            if (level.getBlockState(half).getBlock() instanceof ChimneyBlock) {
                updatePower(level, half);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide()) {
            updatePower(level, pos);
        }
    }

    /** The Chimney blocks of the column through {@code pos}, bottom first, at most {@link SinkBlockEntity#MAX_COLUMN} each way. */
    static List<BlockPos> column(Level level, BlockPos pos) {
        BlockPos bottom = pos;
        for (int step = 0; step < SinkBlockEntity.MAX_COLUMN && level.getBlockState(bottom.below()).getBlock() instanceof ChimneyBlock; step++) {
            bottom = bottom.below();
        }
        List<BlockPos> column = new ArrayList<>();
        column.add(bottom);
        BlockPos cursor = bottom;
        int limit = SinkBlockEntity.MAX_COLUMN + pos.getY() - bottom.getY();
        for (int step = 0; step < limit && level.getBlockState(cursor.above()).getBlock() instanceof ChimneyBlock; step++) {
            cursor = cursor.above();
            column.add(cursor);
        }
        return column;
    }

    /** Sets {@link #POWERED} on every block of the column through {@code pos} when a signal reaches any of them. */
    private static void updatePower(Level level, BlockPos pos) {
        List<BlockPos> column = column(level, pos);
        boolean powered = column.stream().anyMatch(level::hasNeighborSignal);
        for (BlockPos at : column) {
            BlockState state = level.getBlockState(at);
            if (state.getValue(POWERED) != powered) {
                level.setBlock(at, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Carrot: steps the column's smoke. Sneak carrot: steps its redstone mode. Never dismantles. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        Component message;
        if (state.getValue(ON_SINK)) {
            message = Component.translatable("message.servus_foundation.chimney.sink");
        } else if (player != null && player.isShiftKeyDown()) {
            ChimneyRedstone mode = state.getValue(REDSTONE).next();
            setOnColumn(level, pos, REDSTONE, mode);
            message = Component.translatable("message.servus_foundation.chimney.redstone", Component.translatable(mode.translationKey()));
        } else {
            ChimneySmoke smoke = state.getValue(SMOKE).next();
            setOnColumn(level, pos, SMOKE, smoke);
            message = Component.translatable("message.servus_foundation.chimney.smoke", Component.translatable(smoke.translationKey()));
        }
        if (player != null) {
            player.sendOverlayMessage(message);
        }
        return InteractionResult.SUCCESS;
    }

    private static <T extends Comparable<T>> void setOnColumn(Level level, BlockPos pos, Property<T> property, T value) {
        for (BlockPos at : column(level, pos)) {
            BlockState state = level.getBlockState(at);
            if (state.getValue(property) != value) {
                level.setBlock(at, state.setValue(property, value), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Smoke above the top block of a free standing column, by its settings. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(TOP) || !smokes(state)) {
            return;
        }
        ChimneySmoke smoke = state.getValue(SMOKE);
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1);
            double y = pos.getY() + 1.0 + random.nextDouble() * 0.3;
            double z = pos.getZ() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1);
            if (smoke == ChimneySmoke.THIN) {
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.07, 0.0);
            } else {
                SimpleParticleType type = smoke == ChimneySmoke.SIGNAL ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
                level.addAlwaysVisibleParticle(type, true, x, y, z, 0.0, 0.07, 0.0);
            }
        }
    }

    /** Walks down through the column below {@code pos}; a formed Sink at its bottom counts its chimneys again. */
    private static void notifySink(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int step = 0; step <= SinkBlockEntity.MAX_COLUMN; step++) {
            cursor.move(Direction.DOWN);
            BlockState below = level.getBlockState(cursor);
            if (below.getBlock() instanceof ChimneyBlock) {
                continue;
            }
            SinkBlockEntity sink = SinkBlock.controller(level, cursor, below);
            if (sink != null) {
                sink.recountChimney();
            }
            return;
        }
    }
}
