package io.github.dirnei.mc.servus.foundation.machine.sink;

import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * A Sink block. Eight of them in a 2x2x2 cube form the Sink when the carrot
 * clicks a side of the cube; that side becomes the front. The block states
 * alone describe a formed Sink: {@link #FORMED}, the front {@link #FACING} and
 * the block's {@link #PART} (see {@link SinkLayout}). Part 0, the controller,
 * carries {@link #ACTIVE} and owns the budgets. No ticker.
 */
public class SinkBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<SinkBlock> CODEC = simpleCodec(SinkBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, SinkLayout.PARTS - 1);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public SinkBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FORMED, false).setValue(FACING, Direction.NORTH).setValue(PART, 0).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, FACING, PART, ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SinkBlockEntity(pos, state);
    }

    // Layout

    public static boolean isFormed(BlockState state) {
        return state.getBlock() instanceof SinkBlock && state.getValue(FORMED);
    }

    /** The position of {@code part} in the Sink whose controller is at {@code controller}. */
    public static BlockPos partPos(BlockPos controller, Direction facing, int part) {
        SinkLayout.Vec offset = SinkLayout.offset(part, facing.getStepX(), facing.getStepZ());
        return controller.offset(offset.x(), offset.y(), offset.z());
    }

    /** The controller position of the formed Sink block {@code state} at {@code pos}. */
    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        return pos.subtract(partPos(BlockPos.ZERO, state.getValue(FACING), state.getValue(PART)));
    }

    /**
     * The controller of the formed Sink that the block at {@code pos} belongs to, or
     * null when the block is unformed, the controller is not loaded or it is not a
     * formed controller with the same front.
     */
    public static SinkBlockEntity controller(BlockGetter level, BlockPos pos, BlockState state) {
        if (!isFormed(state)) {
            return null;
        }
        BlockPos controllerPos = controllerPos(pos, state);
        if (level instanceof Level world && !world.isLoaded(controllerPos)) {
            return null;
        }
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!isFormed(controllerState) || controllerState.getValue(PART) != SinkLayout.CONTROLLER
                || controllerState.getValue(FACING) != state.getValue(FACING)) {
            return null;
        }
        return level.getBlockEntity(controllerPos) instanceof SinkBlockEntity sink ? sink : null;
    }

    // Forming and unforming

    /**
     * Forms a Sink from the cube whose front contains {@code pos}, clicked on {@code front}.
     * Returns false when no cube of eight unformed Sink blocks lies behind that face.
     */
    public boolean tryForm(Level level, BlockPos pos, Direction front) {
        if (front.getAxis() == Direction.Axis.Y) {
            return false;
        }
        for (SinkLayout.Vec candidate : SinkLayout.candidates(pos.getX(), pos.getY(), pos.getZ(), front.getStepX(), front.getStepZ())) {
            BlockPos controller = new BlockPos(candidate.x(), candidate.y(), candidate.z());
            if (allUnformed(level, controller, front)) {
                form(level, controller, front);
                return true;
            }
        }
        return false;
    }

    private boolean allUnformed(Level level, BlockPos controller, Direction front) {
        for (int part = 0; part < SinkLayout.PARTS; part++) {
            BlockState state = level.getBlockState(partPos(controller, front, part));
            if (!state.is(this) || state.getValue(FORMED)) {
                return false;
            }
        }
        return true;
    }

    private void form(Level level, BlockPos controller, Direction front) {
        for (int part = 0; part < SinkLayout.PARTS; part++) {
            level.setBlock(partPos(controller, front, part),
                    defaultBlockState().setValue(FORMED, true).setValue(FACING, front).setValue(PART, part), Block.UPDATE_ALL);
        }
        for (int part = 0; part < SinkLayout.PARTS; part++) {
            level.invalidateCapabilities(partPos(controller, front, part));
        }
        if (level.getBlockEntity(controller) instanceof SinkBlockEntity sink) {
            sink.recountChimney();
        }
        level.playSound(null, controller, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** A formed block went away: every other block of the same Sink turns back into a casing. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (isFormed(state)) {
            unformRest(level, pos, state);
        }
    }

    private void unformRest(Level level, BlockPos pos, BlockState removed) {
        Direction front = removed.getValue(FACING);
        BlockPos controller = controllerPos(pos, removed);
        for (int part = 0; part < SinkLayout.PARTS; part++) {
            if (part == removed.getValue(PART)) {
                continue;
            }
            BlockPos at = partPos(controller, front, part);
            BlockState state = level.getBlockState(at);
            if (isFormed(state) && state.getValue(FACING) == front && state.getValue(PART) == part) {
                level.setBlock(at, defaultBlockState(), Block.UPDATE_ALL);
                level.invalidateCapabilities(at);
            }
        }
    }

    /** Formed blocks stay where they are; casings follow the block properties. */
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(FORMED) ? PushReaction.BLOCK : null;
    }

    // Active state

    /** Clears the active state once nothing was voided for the active time, or waits for the rest of it. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        int left = level.getBlockEntity(pos) instanceof SinkBlockEntity sink && isFormed(state) ? sink.activeTicksLeft() : 0;
        if (left > 0) {
            level.scheduleTick(pos, this, left);
        } else {
            level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_CLIENTS);
        }
    }

    /** A change right above a top block may change a chimney column. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.UP && !level.isClientSide() && isFormed(state) && SinkLayout.isTop(state.getValue(PART))) {
            SinkBlockEntity sink = controller(level, pos, state);
            if (sink != null) {
                sink.recountChimney();
            }
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    // Interaction

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem) {
            // The carrot item handles the click instead of opening the GUI.
            return InteractionResult.PASS;
        }
        if (!state.getValue(FORMED)) {
            // Casings let held items do their thing, so the cube can be built by clicking on it.
            return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND
                    : InteractionResult.PASS;
        }
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ChimneyBlock) {
            // Chimney blocks go on top without sneaking.
            return InteractionResult.PASS;
        }
        SinkBlockEntity sink = controller(level, pos, state);
        if (sink != null && Transfer.isFluidContainer(stack)
                && FluidUtil.interactWithFluidHandler(player, hand, pos, sink.manualFluidHandler(), null)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        SinkBlockEntity sink = controller(level, pos, state);
        if (sink != null) {
            player.openMenu(sink, sink.getBlockPos());
        } else if (!state.getValue(FORMED)) {
            player.sendOverlayMessage(Component.translatable("message.servus_foundation.sink.casing"));
        }
        return InteractionResult.CONSUME;
    }

    /** Carrot: forms an unformed cube from the clicked side. Sneak carrot: dismantles this one block. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        boolean sneaking = player != null && player.isShiftKeyDown();
        if (!sneaking && state.getValue(FORMED)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (sneaking) {
            Block.popResource(level, pos, new ItemStack(this));
            level.removeBlock(pos, false);
            return InteractionResult.SUCCESS;
        }
        Direction face = context.getClickedFace();
        if (!tryForm(level, pos, face) && player != null) {
            String key = face.getAxis() == Direction.Axis.Y ? "side_face" : "incomplete";
            player.sendOverlayMessage(Component.translatable("message.servus_foundation.sink." + key));
        }
        return InteractionResult.SUCCESS;
    }

    /** Smoke above the tallest chimney column, or above the cube without one, while active. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!isFormed(state) || state.getValue(PART) != SinkLayout.CONTROLLER || !state.getValue(ACTIVE)
                || !(level.getBlockEntity(pos) instanceof SinkBlockEntity sink)) {
            return;
        }
        Direction front = state.getValue(FACING);
        int height = sink.chimneyHeight();
        List<Integer> tops = SinkLayout.topParts();
        int column = height > 0 && sink.chimneyColumn() >= 0 ? sink.chimneyColumn() : tops.get(random.nextInt(tops.size()));
        BlockPos top = partPos(pos, front, column).above(height);
        int count = height > 0 ? 1 + random.nextInt(2) : 1;
        if (height == 0 && random.nextFloat() >= 0.5F) {
            return;
        }
        for (int i = 0; i < count; i++) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    top.getX() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
                    top.getY() + 1.0 + random.nextDouble() * 0.3,
                    top.getZ() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
                    0.0, 0.07, 0.0);
        }
    }
}
