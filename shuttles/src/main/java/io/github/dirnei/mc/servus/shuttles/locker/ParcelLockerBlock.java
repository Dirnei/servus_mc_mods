package io.github.dirnei.mc.servus.shuttles.locker;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.DataPotatoItem;
import io.github.dirnei.mc.servus.foundation.item.PotatoInspectable;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachinePart;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldItem;
import io.github.dirnei.mc.servus.shuttles.rail.RailFacings;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One of the ten blocks of a Parcel Locker, five wide and two high, placed
 * as one piece by {@link ParcelLockerItem}. The dock in the middle of the
 * lower row keeps the block entity; every other part points at it. A part
 * whose neighbour in the locker is gone turns into air, so breaking any part
 * takes the whole locker down, and the dock drops the item and every box's
 * items. A Handheld on the screen above the dock scans it and opens the
 * player's ready boxes, an empty hand opens the locker GUI, and a click on a
 * box takes its items. The screen part shows its {@link LockerScreen}, the
 * dock whether shuttles are bringing orders; doors, their lamps and the items
 * of open boxes are drawn by the dock's renderer.
 */
public class ParcelLockerBlock extends BaseEntityBlock implements PotatoInspectable, NamedMachinePart {

    public static final MapCodec<ParcelLockerBlock> CODEC = simpleCodec(ParcelLockerBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<LockerPart> PART = EnumProperty.create("part", LockerPart.class);
    public static final EnumProperty<LockerScreen> SCREEN = EnumProperty.create("screen", LockerScreen.class);
    public static final BooleanProperty LOADING = BooleanProperty.create("loading");
    private static final float SCAN_PITCH = 2.0F;
    private static final float BUZZ_PITCH = 0.6F;
    private static final float TAKE_VOLUME = 0.4F;
    private static final float TAKE_PITCH = 1.2F;

    public ParcelLockerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, LockerPart.HEAD_DOCK)
                .setValue(SCREEN, LockerScreen.IDLE).setValue(LOADING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, SCREEN, LOADING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == LockerPart.HEAD_DOCK ? new ParcelLockerBlockEntity(pos, state) : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** The dock ticks on clients only, to swing the doors and ring them. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide() || state.getValue(PART) != LockerPart.HEAD_DOCK) {
            return null;
        }
        return createTickerHelper(type, ServusShuttlesBlockEntities.PARCEL_LOCKER.get(), ParcelLockerBlockEntity::clientTick);
    }

    /** The screen's fall back from a passing state. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) == LockerPart.HEAD_SCREEN) {
            ParcelLockerBlockEntity locker = locker(level, pos);
            if (locker != null) {
                locker.refreshScreen();
            }
        }
    }

    /** Where the dock of the locker the part at {@code pos} belongs to stands. */
    public static BlockPos dockPos(BlockPos pos, BlockState state) {
        return state.getValue(PART).dock(pos, state.getValue(FACING));
    }

    /** The locker any part at {@code pos} belongs to, or null when there is none or its dock is not loaded. */
    public static @Nullable ParcelLockerBlockEntity locker(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ParcelLockerBlock)) {
            return null;
        }
        BlockPos dock = dockPos(pos, state);
        if (level instanceof Level world && !world.isLoaded(dock)) {
            return null;
        }
        return level.getBlockEntity(dock) instanceof ParcelLockerBlockEntity locker ? locker : null;
    }

    @Override
    public @Nullable BlockPos namedMachinePos(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof ParcelLockerBlock ? dockPos(pos, state) : null;
    }

    /** Null without room for all ten parts, so a plain placement places nothing; {@link ParcelLockerItem} says why. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = RailFacings.frontToRail(context, RailFacings.ANY_RAIL);
        if (ParcelLockerItem.refusal(context, facing) != null) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    /** Places the nine parts around the dock. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        Direction facing = state.getValue(FACING);
        for (LockerPart part : LockerPart.values()) {
            if (part != LockerPart.HEAD_DOCK) {
                level.setBlock(part.at(pos, facing), state.setValue(PART, part), Block.UPDATE_ALL);
            }
        }
    }

    /** A part whose neighbour in the locker is no longer that part turns into air, so a locker never stands half. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        LockerPart part = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        BlockPos dock = part.dock(pos, facing);
        for (LockerPart other : LockerPart.values()) {
            if (other != part && other.at(dock, facing).equals(neighborPos)
                    && !(neighborState.is(this) && neighborState.getValue(PART) == other && neighborState.getValue(FACING) == facing)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** A creative player breaking any part gets no locker item; the boxes still drop their items. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            ParcelLockerBlockEntity locker = locker(level, pos);
            if (locker != null) {
                locker.dropNoItem();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    /**
     * A Handheld on the screen scans it; any hand on a box takes from it.
     * The potato passes to its report; other items on the screen fall to
     * the empty hand, which opens the GUI.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof DataPotatoItem) {
            return InteractionResult.PASS;
        }
        LockerPart part = state.getValue(PART);
        if (part == LockerPart.HEAD_SCREEN) {
            return stack.getItem() instanceof HandheldItem ? scan(level, pos, player) : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        LockerLayout.Box box = part.box((hitResult.getLocation().y - pos.getY()) * LockerLayout.BLOCK);
        ParcelLockerBlockEntity locker = locker(level, pos);
        if (box == null || locker == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return locker.shown(box.index()).state() == null ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        return switch (locker.take(player, box.index())) {
            case FREE -> InteractionResult.PASS;
            case TAKEN -> {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, TAKE_VOLUME, TAKE_PITCH);
                yield InteractionResult.SUCCESS;
            }
            case NOT_YOURS -> {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, BUZZ_PITCH);
                player.sendOverlayMessage(Component.translatable("message.servus_shuttles.parcel_locker.not_yours", box.id()));
                yield InteractionResult.SUCCESS;
            }
            case CLOSED -> {
                player.sendOverlayMessage(Component.translatable("message.servus_shuttles.parcel_locker.scan_to_open"));
                yield InteractionResult.SUCCESS;
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(PART) != LockerPart.HEAD_SCREEN) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ParcelLockerBlockEntity locker = locker(level, pos);
            if (locker != null) {
                serverPlayer.openMenu(locker, buf -> locker.writeOpening(buf, serverPlayer.getUUID()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Opens the player's ready boxes and greets them on the screen, with a chirp and a line on the action bar. */
    private static InteractionResult scan(Level level, BlockPos pos, Player player) {
        ParcelLockerBlockEntity locker = locker(level, pos);
        if (level.isClientSide() || locker == null) {
            return InteractionResult.SUCCESS;
        }
        List<Integer> opened = locker.scan(player);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.8F, SCAN_PITCH);
        if (opened.isEmpty()) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, BUZZ_PITCH);
            player.sendOverlayMessage(Component.translatable("message.servus_shuttles.parcel_locker.nothing", locker.label()));
        } else {
            player.sendOverlayMessage(Component.translatable("message.servus_shuttles.parcel_locker.opened", locker.label(),
                    String.join(", ", opened.stream().map(box -> LockerLayout.BOXES.get(box).id()).toList())));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public List<Component> potatoReport(Level level, BlockPos pos, BlockState state, Vec3 hit) {
        ParcelLockerBlockEntity locker = locker(level, pos);
        return locker == null ? List.of() : ParcelLockerReport.of(locker);
    }
}
