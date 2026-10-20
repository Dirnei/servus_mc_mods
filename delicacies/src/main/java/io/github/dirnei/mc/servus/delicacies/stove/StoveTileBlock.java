package io.github.dirnei.mc.servus.delicacies.stove;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.delicacies.firebox.ElementUse;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenStatsManager;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A Stove Tile. A straight row of loose tiles forms a Tiled Stove when the carrot clicks one of them: the clicked tile
 * gets the firebox door on {@link #FACING}, every tile a cooking plate on top. Breaking any tile unforms the stove.
 * With a Heating Element the door shows coils and a thermometer and the {@link #PORT} tile a power port.
 */
public class StoveTileBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<StoveTileBlock> CODEC = simpleCodec(StoveTileBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** The tile with the firebox door, the master of the stove. */
    public static final BooleanProperty DOOR = BooleanProperty.create("door");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** The stove's front, where the door is. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** The stove holds a Heating Element. */
    public static final BooleanProperty HEATER = BooleanProperty.create("heater");
    /** The tile at the end right of the front, which carries the power port. */
    public static final BooleanProperty PORT = BooleanProperty.create("port");
    /** Light around a burning door. */
    public static final int DOOR_LIGHT = 13;
    /** Light around a door whose coils glow. */
    public static final int HEATER_LIGHT = 7;

    public StoveTileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(DOOR, false).setValue(LIT, false)
                .setValue(FACING, Direction.NORTH).setValue(HEATER, false).setValue(PORT, false));
    }

    /** The light a tile in {@code state} gives. */
    public static int lightLevel(BlockState state) {
        if (!state.getValue(FORMED) || !state.getValue(DOOR) || !state.getValue(LIT)) {
            return 0;
        }
        return state.getValue(HEATER) ? HEATER_LIGHT : DOOR_LIGHT;
    }

    /** The face the power port of a {@link #PORT} tile is on: the end to the right seen from the front. */
    public static Direction portFace(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, DOOR, LIT, FACING, HEATER, PORT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoveTileBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || !state.getValue(FORMED) || !state.getValue(DOOR)) {
            return null;
        }
        return createTickerHelper(type, ServusDelicaciesBlockEntities.STOVE_TILE.get(), (tickLevel, pos, tickState, tile) -> tile.serverTick());
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(FORMED) ? PushReaction.BLOCK : null;
    }

    /**
     * A Heating Element on a formed stove goes into its firebox, and so does fuel while it has none; the carrot and
     * other items pass.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || !state.getValue(FORMED)
                || !(level.getBlockEntity(pos) instanceof StoveTileBlockEntity tile)) {
            return InteractionResult.PASS;
        }
        if (ElementUse.isElement(stack)) {
            if (!level.isClientSide() && tile.master() != null) {
                ElementUse.install(level, pos, player, stack, tile::installElement);
            }
            return InteractionResult.SUCCESS;
        }
        if (!tile.isFuel(stack)) {
            return InteractionResult.PASS;
        }
        if (tile.hasElement()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(ElementUse.fuelRefused());
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            ItemStack left = tile.addFuel(stack.copy());
            if (left.getCount() != stack.getCount()) {
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, left);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 0.8F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand tells how the firebox heats; sneaking, it takes the Heating Element out. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof StoveTileBlockEntity tile) {
            if (player.isShiftKeyDown() && tile.master() != null) {
                ElementUse.takeOut(level, pos, player, tile::takeElement);
            } else {
                player.sendOverlayMessage(tile.status());
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Carrot: forms the row through the clicked tile. Sneak carrot: dismantles the tile. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        boolean sneaking = player != null && player.isShiftKeyDown();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (sneaking) {
            Block.popResource(level, pos, new ItemStack(this));
            level.removeBlock(pos, false);
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(FORMED)) {
            if (level.getBlockEntity(pos) instanceof StoveTileBlockEntity tile && tile.master() == null) {
                tile.unformStray();
            } else if (player != null && level.getBlockEntity(pos) instanceof StoveTileBlockEntity tile) {
                player.sendOverlayMessage(tile.status());
            }
            return InteractionResult.SUCCESS;
        }
        Direction face = context.getClickedFace();
        Direction front = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        StoveRow.Result result = tryForm(level, pos, front);
        if (player != null) {
            player.sendOverlayMessage(message(result));
        }
        return InteractionResult.SUCCESS;
    }

    /** Forms the row of loose tiles through {@code pos} across {@code front}, with the door on {@code pos}. */
    public static StoveRow.Result tryForm(Level level, BlockPos pos, Direction front) {
        Direction along = front.getClockWise();
        int maxLength = KitchenStatsManager.INSTANCE.stats().stoveMaxLength();
        StoveRow.Result result = StoveRow.find(offset -> isLoose(level, pos.relative(along, offset)), maxLength);
        if (result.formed()) {
            form(level, pos, front, along, result);
        }
        return result;
    }

    private static boolean isLoose(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof StoveTileBlock && !state.getValue(FORMED);
    }

    private static void form(Level level, BlockPos master, Direction front, Direction along, StoveRow.Result row) {
        for (int offset = row.from(); offset <= row.to(); offset++) {
            BlockPos at = master.relative(along, offset);
            BlockState state = level.getBlockState(at);
            level.setBlock(at, state.setValue(FORMED, true).setValue(DOOR, offset == 0).setValue(LIT, false)
                    .setValue(FACING, front).setValue(HEATER, false).setValue(PORT, offset == row.from()), Block.UPDATE_ALL);
            if (level.getBlockEntity(at) instanceof StoveTileBlockEntity tile) {
                if (offset == 0) {
                    tile.becomeMaster(along, row.from(), row.length());
                } else {
                    tile.joinMaster(master);
                }
            }
        }
        for (int offset = row.from(); offset <= row.to(); offset++) {
            level.invalidateCapabilities(master.relative(along, offset));
        }
        level.playSound(null, master, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** What the carrot click says: formed with the length, or why not. */
    public static Component message(StoveRow.Result result) {
        return switch (result.outcome()) {
            case FORMED -> Component.translatable("message.servus_delicacies.stove.formed", result.length());
            case TOO_SHORT -> Component.translatable("message.servus_delicacies.stove.too_short", StoveRow.MIN_LENGTH);
            case TOO_LONG -> Component.translatable("message.servus_delicacies.stove.too_long",
                    KitchenStatsManager.INSTANCE.stats().stoveMaxLength());
        };
    }

    /** Flames and smoke at a door with a wood fire, and the crackle of a furnace now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(DOOR) || !state.getValue(LIT) || state.getValue(HEATER)) {
            return;
        }
        Direction front = state.getValue(FACING);
        double x = pos.getX() + 0.5 + front.getStepX() * 0.52;
        double z = pos.getZ() + 0.5 + front.getStepZ() * 0.52;
        double side = (random.nextDouble() - 0.5) * 0.4;
        double y = pos.getY() + 0.35 + random.nextDouble() * 0.2;
        x += front.getClockWise().getStepX() * side;
        z += front.getClockWise().getStepZ() * side;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                    1.0F, 1.0F, false);
        }
        if (random.nextDouble() < 0.4) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0, 0);
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.1, z, 0, 0.01, 0);
        }
    }
}
