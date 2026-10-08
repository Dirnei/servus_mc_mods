package io.github.dirnei.mc.servus.storage.cpu;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Crafting CPU block. 27 of them in a 3x3x3 cube form a CPU when the
 * carrot clicks one; {@link #FORMED} and {@link #PART} describe the formed
 * cube (see {@link CpuShape}). Part 0, the lowest corner, is the master that
 * owns the work orders.
 */
public class CpuBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<CpuBlock> CODEC = simpleCodec(CpuBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, CpuShape.PARTS - 1);

    public CpuBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(PART, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CpuBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || !state.getValue(FORMED) || state.getValue(PART) != 0) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> {
            if (entity instanceof CpuBlockEntity cpu) {
                cpu.serverTick();
            }
        };
    }

    // Layout

    public static boolean isFormed(BlockState state) {
        return state.getBlock() instanceof CpuBlock && state.getValue(FORMED);
    }

    /** The master position of the formed block {@code state} at {@code pos}. */
    public static BlockPos masterPos(BlockPos pos, BlockState state) {
        CpuShape.Vec offset = CpuShape.offset(state.getValue(PART));
        return pos.offset(-offset.x(), -offset.y(), -offset.z());
    }

    /** The master of the formed CPU the block at {@code pos} belongs to, or null. */
    public static CpuBlockEntity master(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isFormed(state)) {
            return null;
        }
        BlockPos master = masterPos(pos, state);
        if (level instanceof Level world && !world.isLoaded(master)) {
            return null;
        }
        BlockState masterState = level.getBlockState(master);
        if (!isFormed(masterState) || masterState.getValue(PART) != 0) {
            return null;
        }
        return level.getBlockEntity(master) instanceof CpuBlockEntity cpu ? cpu : null;
    }

    private static BlockPos partPos(BlockPos origin, int part) {
        CpuShape.Vec offset = CpuShape.offset(part);
        return origin.offset(offset.x(), offset.y(), offset.z());
    }

    // Forming and unforming

    /** Forms the first full cube of unformed CPU blocks that contains {@code pos}; false when there is none. */
    public boolean tryForm(Level level, BlockPos pos) {
        for (CpuShape.Vec origin : CpuShape.origins(pos.getX(), pos.getY(), pos.getZ())) {
            BlockPos corner = new BlockPos(origin.x(), origin.y(), origin.z());
            if (allUnformed(level, corner)) {
                form(level, corner);
                return true;
            }
        }
        return false;
    }

    private boolean allUnformed(Level level, BlockPos origin) {
        for (int part = 0; part < CpuShape.PARTS; part++) {
            BlockState state = level.getBlockState(partPos(origin, part));
            if (!state.is(this) || state.getValue(FORMED)) {
                return false;
            }
        }
        return true;
    }

    private void form(Level level, BlockPos origin) {
        for (int part = 0; part < CpuShape.PARTS; part++) {
            level.setBlock(partPos(origin, part), defaultBlockState().setValue(FORMED, true).setValue(PART, part), Block.UPDATE_ALL);
        }
        for (int part = 0; part < CpuShape.PARTS; part++) {
            level.invalidateCapabilities(partPos(origin, part));
        }
        level.playSound(null, origin.offset(1, 1, 1), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    /** A formed block went away: the master cancels its orders and every other block of the cube unforms. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && isFormed(state) && !newState.is(this)) {
            unform(level, masterPos(pos, state), state.getValue(PART));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void unform(Level level, BlockPos origin, int removedPart) {
        if (level.getBlockEntity(origin) instanceof CpuBlockEntity master) {
            master.cancelAll();
        }
        for (int part = 0; part < CpuShape.PARTS; part++) {
            if (part == removedPart) {
                continue;
            }
            BlockPos at = partPos(origin, part);
            BlockState state = level.getBlockState(at);
            if (isFormed(state) && state.getValue(PART) == part) {
                level.setBlock(at, defaultBlockState(), Block.UPDATE_ALL);
                level.invalidateCapabilities(at);
            }
        }
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(FORMED) ? PushReaction.BLOCK : null;
    }

    // Interaction

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        return stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand shows the CPU's slots, or how to form it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel) {
            CpuBlockEntity master = master(level, pos);
            player.displayClientMessage(master != null ? master.summary()
                    : Component.translatable("message.servus_storage.cpu.casing"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Carrot: forms the cube around the clicked block. Sneak carrot: dismantles this block. */
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
        boolean formed = tryForm(level, pos);
        if (player != null) {
            player.displayClientMessage(Component.translatable(formed ? "message.servus_storage.cpu.formed"
                    : "message.servus_storage.cpu.incomplete"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
