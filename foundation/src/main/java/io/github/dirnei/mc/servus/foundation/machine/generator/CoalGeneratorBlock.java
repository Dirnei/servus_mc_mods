package io.github.dirnei.mc.servus.foundation.machine.generator;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.TwoHalves;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachinePart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Coal Generator: a dark steel cube with a square chimney centred on top,
 * two blocks tall and placed and broken as one through {@link TwoHalves}. The
 * cube is the machine; the chimney mirrors the cube's facing and lit state,
 * smokes while it burns and forwards clicks to the cube.
 */
public class CoalGeneratorBlock extends MachineBlock implements NamedMachinePart {

    /** The chimney stands for the machine in the cube below. */
    @Override
    public BlockPos namedMachinePos(Level level, BlockPos pos, BlockState state) {
        return TwoHalves.lowerPos(state, pos);
    }

    public static final MapCodec<CoalGeneratorBlock> CODEC = simpleCodec(CoalGeneratorBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = TwoHalves.HALF;

    /** The chimney stack and its overhanging cap, in pixels, centred on the block. */
    public static final float[] STACK = {5, 0, 5, 11, 14, 11};
    public static final float[] CAP = {4, 14, 4, 12, 16, 12};

    private static final VoxelShape CHIMNEY = Shapes.or(
            Block.box(STACK[0], STACK[1], STACK[2], STACK[3], STACK[4], STACK[5]),
            Block.box(CAP[0], CAP[1], CAP[2], CAP[3], CAP[4], CAP[5]));

    public CoalGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return MachineContent.COAL_GENERATOR_ENTITY.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    public static boolean isUpper(BlockState state) {
        return TwoHalves.isUpper(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isUpper(state) ? CHIMNEY : Shapes.block();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return TwoHalves.canPlace(context) ? super.getStateForPlacement(context) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        TwoHalves.placeUpper(level, pos, upperOf(state));
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    /** The chimney matching a cube: same facing, same lit state. */
    public BlockState upperOf(BlockState lower) {
        return defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER).setValue(FACING, lower.getValue(FACING))
                .setValue(LIT, lower.getValue(LIT));
    }

    /** The halves stay together; the chimney follows the cube's facing and lit state, so it smokes while the cube burns. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        BlockState kept = TwoHalves.keepTogether(this, state, direction, neighborState);
        if (kept == null) {
            return state;
        }
        if (kept.isAir() || !isUpper(kept)) {
            return kept;
        }
        return kept.setValue(FACING, neighborState.getValue(FACING)).setValue(LIT, neighborState.getValue(LIT));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        TwoHalves.breakPartner(this, level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isUpper(state) ? null : super.newBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return super.useWithoutItem(state, level, TwoHalves.lowerPos(state, pos), player, hitResult);
    }

    /** On the cube the carrot cycles sides as on any machine; on the chimney it only dismantles while sneaking. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        if (isUpper(state) && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown())) {
            return InteractionResult.SUCCESS;
        }
        return TwoHalves.forwardWrench(context, state, super::onWrench);
    }

    /** Smoke from the chimney cap while the generator burns. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!isUpper(state) || !state.getValue(LIT) || random.nextFloat() >= 0.5F) {
            return;
        }
        level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                pos.getX() + 0.5 + (random.nextDouble() - 0.5) / 16.0,
                pos.getY() + CAP[4] / 16.0 + random.nextDouble() * 0.1,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) / 16.0,
                0.0, 0.05, 0.0);
    }
}
