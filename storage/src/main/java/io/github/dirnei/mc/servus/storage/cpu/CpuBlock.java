package io.github.dirnei.mc.servus.storage.cpu;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import io.github.dirnei.mc.servus.foundation.wood.Wood;

/**
 * A Crafting CPU block. CPU and Buffer blocks that fill a solid box of at
 * least 2x2x2 form a CPU when the carrot clicks one (see {@link CpuBox});
 * {@link #FORMED} and {@link #PART} (the place class in the box, see
 * {@link CpuShape}) describe the formed box. Part 0, the lowest corner, is
 * the master that owns the work orders and the buffer; every other block
 * saves the offset to it.
 */
public class CpuBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<CpuBlock> CODEC = simpleCodec(CpuBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, CpuShape.PARTS - 1);
    /** A formed box whose CPU has open orders: its lights run. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public CpuBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(PART, 0).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, PART, ACTIVE);
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

    /** The master of the formed CPU the block at {@code pos} belongs to, or null. */
    public static CpuBlockEntity master(BlockGetter level, BlockPos pos) {
        if (!isFormed(level.getBlockState(pos)) || !(level.getBlockEntity(pos) instanceof CpuBlockEntity block)) {
            return null;
        }
        BlockPos master = block.masterPos();
        if (master == null || level instanceof Level world && !world.isLoaded(master)) {
            return null;
        }
        BlockState masterState = level.getBlockState(master);
        if (!isFormed(masterState) || masterState.getValue(PART) != 0
                || !(level.getBlockEntity(master) instanceof CpuBlockEntity cpu) || !cpu.isMaster()) {
            return null;
        }
        return cpu;
    }

    // Forming and unforming

    /** The unformed CPU and Buffer blocks joined face to face to {@code start}, at most {@code limit}. */
    static Set<BlockPos> joined(Level level, BlockPos start, int limit) {
        Set<BlockPos> found = new LinkedHashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        found.add(start.immutable());
        queue.add(start.immutable());
        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos at = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = at.relative(direction);
                if (found.contains(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof CpuBlock && !state.getValue(FORMED)) {
                    found.add(next);
                    queue.add(next);
                    if (found.size() >= limit) {
                        break;
                    }
                }
            }
        }
        return found;
    }

    /** Checks the blocks joined to {@code pos} and forms them when they fill a box. */
    public static CpuBox.Result tryForm(Level level, BlockPos pos) {
        int maxBlocks = StorageStatsManager.INSTANCE.cpuMaxBlocks();
        Set<BlockPos> blocks = joined(level, pos, maxBlocks + 1);
        Set<CpuShape.Vec> joined = new HashSet<>();
        Set<CpuShape.Vec> cpuBlocks = new HashSet<>();
        for (BlockPos at : blocks) {
            CpuShape.Vec vec = new CpuShape.Vec(at.getX(), at.getY(), at.getZ());
            joined.add(vec);
            if (!(level.getBlockState(at).getBlock() instanceof CpuBufferBlock)) {
                cpuBlocks.add(vec);
            }
        }
        CpuBox.Result result = CpuBox.check(joined, cpuBlocks, maxBlocks);
        if (result.formed()) {
            form(level, result.box());
        }
        return result;
    }

    private static void form(Level level, CpuBox.Box box) {
        BlockPos corner = new BlockPos(box.corner().x(), box.corner().y(), box.corner().z());
        List<BlockPos> all = positions(corner, box.size());
        for (BlockPos at : all) {
            BlockState state = level.getBlockState(at);
            CpuShape.Vec offset = new CpuShape.Vec(at.getX() - corner.getX(), at.getY() - corner.getY(), at.getZ() - corner.getZ());
            level.setBlock(at, state.getBlock().defaultBlockState().setValue(FORMED, true).setValue(PART, box.part(offset)),
                    Block.UPDATE_ALL);
            if (level.getBlockEntity(at) instanceof CpuBlockEntity block) {
                block.joinBox(corner, at.equals(corner) ? box : null);
            }
        }
        for (BlockPos at : all) {
            level.invalidateCapabilities(at);
        }
        BlockPos centre = corner.offset(box.size().x() / 2, box.size().y() / 2, box.size().z() / 2);
        level.playSound(null, centre, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    /** Every position of a box, lowest corner first. */
    static List<BlockPos> positions(BlockPos corner, CpuShape.Vec size) {
        List<BlockPos> list = new ArrayList<>(size.x() * size.y() * size.z());
        for (int y = 0; y < size.y(); y++) {
            for (int z = 0; z < size.z(); z++) {
                for (int x = 0; x < size.x(); x++) {
                    list.add(corner.offset(x, y, z));
                }
            }
        }
        return list;
    }

    /**
     * Unforms the box of a master: every block of it except {@code removed}
     * goes back to its unformed state and forgets its master.
     */
    static void unformBlocks(Level level, CpuBlockEntity master, BlockPos removed) {
        CpuShape.Vec size = master.boxSize();
        if (size == null) {
            return;
        }
        BlockPos corner = master.getBlockPos();
        List<BlockPos> all = positions(corner, size);
        for (BlockPos at : all) {
            if (at.equals(removed) || !level.isLoaded(at)) {
                continue;
            }
            BlockState state = level.getBlockState(at);
            if (isFormed(state) && level.getBlockEntity(at) instanceof CpuBlockEntity block && corner.equals(block.masterPos())) {
                block.leaveBox();
                level.setBlock(at, state.getBlock().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        for (BlockPos at : all) {
            if (level.isLoaded(at)) {
                level.invalidateCapabilities(at);
            }
        }
    }

    /**
     * A formed block went away without its block entity unforming the box
     * first (a removal that skips block entity side effects): formed
     * neighbours unform the rest of the box, through their master when it is
     * still there.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (!isFormed(state)) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            BlockState neighbour = level.getBlockState(next);
            if (!isFormed(neighbour) || !(level.getBlockEntity(next) instanceof CpuBlockEntity block)) {
                continue;
            }
            CpuBlockEntity master = master(level, next);
            if (master != null && master.boxContains(pos)) {
                master.unform(pos);
            } else if (master == null && pos.equals(block.masterPos())) {
                unformOrphans(level, next, block.masterPos());
            }
        }
    }

    /** Unforms the formed blocks joined to {@code start} that name a master that is gone. */
    private static void unformOrphans(Level level, BlockPos start, BlockPos masterPos) {
        int limit = StorageStatsManager.INSTANCE.cpuMaxBlocks();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() <= limit) {
            BlockPos at = queue.poll();
            BlockState state = level.getBlockState(at);
            if (!isFormed(state) || !(level.getBlockEntity(at) instanceof CpuBlockEntity block)
                    || !masterPos.equals(block.masterPos())) {
                continue;
            }
            block.leaveBox();
            level.setBlock(at, state.getBlock().defaultBlockState(), Block.UPDATE_ALL);
            level.invalidateCapabilities(at);
            for (Direction direction : Direction.values()) {
                BlockPos next = at.relative(direction);
                if (level.isLoaded(next) && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(FORMED) ? PushReaction.BLOCK : null;
    }

    // Interaction

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        // The carrot forms the box and a CPU or Buffer block places the next one; any other item shows
        // the summary like a chest, sneaking uses the item.
        boolean cpuBlock = stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof CpuBlock;
        return stack.getItem() instanceof ConfigurationCarrotItem || cpuBlock ? InteractionResult.PASS
                : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** An empty hand shows the CPU's slots, or how to form it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel) {
            CpuBlockEntity master = master(level, pos);
            player.sendOverlayMessage(master != null ? master.summary()
                    : Component.translatable("message.servus_storage.cpu.casing"));
        }
        return InteractionResult.SUCCESS;
    }

    /** What the carrot click says: formed with the box's size, or why not. */
    static Component message(CpuBox.Result result) {
        if (result.formed()) {
            CpuShape.Vec size = result.box().size();
            return Component.translatable("message.servus_storage.cpu.formed", size.x(), size.y(), size.z(),
                    result.box().cpuBlocks(), result.box().bufferBlocks());
        }
        String key = "message.servus_storage.cpu." + result.reason().id();
        return switch (result.reason()) {
            case TOO_BIG -> Component.translatable(key, StorageStatsManager.INSTANCE.cpuMaxBlocks());
            case GAP -> Component.translatable(key, result.gap().x(), result.gap().y(), result.gap().z());
            default -> Component.translatable(key);
        };
    }

    /** Carrot: forms the box around the clicked block. Sneak carrot: dismantles this block. */
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
            Block.popResource(level, pos, Wood.stamp(new ItemStack(this), level.getBlockEntity(pos)));
            level.removeBlock(pos, false);
            return InteractionResult.SUCCESS;
        }
        CpuBox.Result result = tryForm(level, pos);
        if (player != null) {
            player.sendOverlayMessage(message(result));
        }
        return InteractionResult.SUCCESS;
    }
}
