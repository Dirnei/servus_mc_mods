package io.github.dirnei.mc.servus.foundation.drive;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Hand Crank: sits on a horizontal side of a block that takes drive and
 * turns it while a player holds right click on it. {@link #FACING} points from
 * the driven block to the crank; {@link #TURNING} is the server's word on whether
 * it turns. It drops off when the driven block goes.
 */
public class HandCrankBlock extends BaseEntityBlock {

    public static final MapCodec<HandCrankBlock> CODEC = simpleCodec(HandCrankBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty TURNING = BooleanProperty.create("turning");

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES.put(facing, PartShapes.shapeOf(HandCrankParts.shapeParts(), facing));
        }
    }

    public HandCrankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TURNING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TURNING);
    }

    /** Whether the block on the {@code facing} side's opposite takes drive through the side facing the crank. */
    private static boolean drivenBehind(Level level, BlockPos pos, Direction facing) {
        return level.getCapability(ServusFoundationCapabilities.DRIVE, pos.relative(facing.getOpposite()), facing) != null;
    }

    /** Only against a horizontal side that takes drive; anywhere else nothing is placed. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isVertical() || !drivenBehind(context.getLevel(), context.getClickedPos(), face)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, face);
    }

    /** Asked only of a real level: capabilities need one. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !(level instanceof Level real) || drivenBehind(real, pos, state.getValue(FACING));
    }

    /** The crank drops off when the block behind it no longer takes drive. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Whatever the hand holds, a click turns the crank. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** A click keeps the crank turning a moment; holding the use key repeats it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HandCrankBlockEntity crank) {
            crank.click(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HandCrankBlockEntity(pos, state);
    }

    /** The server drives and decides whether the crank turns; the client runs the arm's flywheel. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return createTickerHelper(type, MachineContent.HAND_CRANK_ENTITY.get(), (tickLevel, pos, tickState, crank) -> crank.serverTick());
        }
        return createTickerHelper(type, MachineContent.HAND_CRANK_ENTITY.get(), (tickLevel, pos, tickState, crank) -> crank.clientTick());
    }
}
