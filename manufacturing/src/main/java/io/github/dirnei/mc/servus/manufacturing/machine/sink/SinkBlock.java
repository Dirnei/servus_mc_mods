package io.github.dirnei.mc.servus.manufacturing.machine.sink;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.lib.block.SideConfigWrench;
import io.github.dirnei.mc.servus.lib.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.lib.item.Wrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * The Sink block: faces the player, active while it voided something
 * recently, no ticker. The carrot cycles sides and sneak carrot dismantles.
 */
public class SinkBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<SinkBlock> CODEC = simpleCodec(SinkBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public SinkBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SinkBlockEntity(pos, state);
    }

    /** Clears the active state once nothing was voided for the active time, or waits for the rest of it. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        int left = level.getBlockEntity(pos) instanceof SinkBlockEntity sink ? sink.activeTicksLeft() : 0;
        if (left > 0) {
            level.scheduleTick(pos, this, left);
        } else {
            level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_CLIENTS);
        }
    }

    /** A change right above may change the chimney column. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP && !level.isClientSide() && level.getBlockEntity(pos) instanceof SinkBlockEntity sink) {
            sink.recountChimney();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem) {
            // The carrot item handles the click instead of opening the GUI.
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof SinkBlockEntity sink
                && FluidUtil.getFluidHandler(stack).isPresent()
                && FluidUtil.interactWithFluidHandler(player, hand, sink.manualFluidHandler())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SinkBlockEntity sink) {
            player.openMenu(sink, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        if (level.getBlockEntity(context.getClickedPos()) instanceof SinkBlockEntity sink) {
            // Nothing to spill: the Sink holds nothing.
            return SideConfigWrench.use(context, this, sink, () -> {
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Smoke from the top of the chimney column, or from the Sink itself without one, while active. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextFloat() >= 0.35F) {
            return;
        }
        BlockPos top = pos.above(SinkBlockEntity.countColumn(level, pos));
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    top.getX() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
                    top.getY() + 1.0 + random.nextDouble() * 0.3,
                    top.getZ() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
                    0.0, 0.07, 0.0);
        }
    }
}
