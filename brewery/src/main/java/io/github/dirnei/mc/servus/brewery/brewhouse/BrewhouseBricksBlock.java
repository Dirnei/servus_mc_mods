package io.github.dirnei.mc.servus.brewery.brewhouse;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Kind;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlockEntities;
import java.util.Locale;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Brewhouse Bricks: the firebox of the Brewhouse. Formed, the front bricks carry the fire doors, the four on the left
 * carry the kettle and take fuel, and the two on the right are the kiln trays.
 */
public class BrewhouseBricksBlock extends BrewhousePartBlock {

    public static final MapCodec<BrewhouseBricksBlock> CODEC = simpleCodec(BrewhouseBricksBlock::new);
    public static final EnumProperty<BrewhousePart> PART =
            EnumProperty.create("part", BrewhousePart.class, part -> part.cell().kind() == Kind.BRICK);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** What a kiln tray shows; unused on the other bricks. */
    public static final EnumProperty<TrayLook> TRAY = EnumProperty.create("tray", TrayLook.class);
    /** Light around a burning door. */
    public static final int DOOR_LIGHT = 13;

    public BrewhouseBricksBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(PART, BrewhousePart.KETTLE_FRONT_LEFT)
                .setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(TRAY, TrayLook.EMPTY));
    }

    /** The light a brick in {@code state} gives: a burning door glows. */
    public static int lightLevel(BlockState state) {
        return state.getValue(FORMED) && state.getValue(LIT) && state.getValue(PART).cell().hasDoor() ? DOOR_LIGHT : 0;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, PART, FACING, LIT, TRAY);
    }

    @Override
    public Kind kind() {
        return Kind.BRICK;
    }

    @Override
    public BrewhousePart part(BlockState state) {
        return state.getValue(PART);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || !state.getValue(FORMED) || state.getValue(PART).cell() != BrewhouseLayout.MASTER) {
            return null;
        }
        return createTickerHelper(type, ServusBreweryBlockEntities.BREWHOUSE.get(), (tickLevel, pos, tickState, part) -> part.serverTick());
    }

    /** A kiln tray toggles its mode; other bricks tell the fire. */
    @Override
    protected Component wrenchFormed(BrewhouseBlockEntity part, BlockState state) {
        KilnTray tray = part.tray();
        if (tray == null) {
            return part.fireStatus();
        }
        return Component.translatable("message.servus_brewery.kiln.mode." + tray.toggleMode().name().toLowerCase(Locale.ROOT));
    }

    /** Malt on a tray, fuel on any brick; the carrot and other items pass. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        BrewhouseBlockEntity part = formedPart(level, pos, state);
        if (isCarrot(stack) || part == null) {
            return InteractionResult.PASS;
        }
        KilnTray tray = part.tray();
        if (tray != null && tray.accepts(stack)) {
            if (!level.isClientSide()) {
                int spread = tray.spread(stack);
                if (spread == 0) {
                    return InteractionResult.FAIL;
                }
                stack.consume(spread, player);
                level.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!part.isFuel(stack)) {
            return tray != null ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack left = part.addFuel(stack.copy());
            if (left.getCount() != stack.getCount()) {
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, left);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 0.8F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand takes a tray's malt, or tells the fire. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel) || !(level.getBlockEntity(pos) instanceof BrewhouseBlockEntity part)) {
            return InteractionResult.SUCCESS;
        }
        KilnTray tray = part.tray();
        if (tray != null && !tray.stack().isEmpty()) {
            player.getInventory().placeItemBackInInventory(tray.takeAll());
            level.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            player.sendOverlayMessage(part.fireStatus());
        }
        return InteractionResult.SUCCESS;
    }

    /** Flames and crackle at a burning door, and a little vapour over a tray that dries. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FORMED) || !state.getValue(LIT)) {
            return;
        }
        Cell cell = state.getValue(PART).cell();
        Direction facing = state.getValue(FACING);
        if (cell.hasDoor() && random.nextDouble() < 0.3) {
            boolean kiln = cell.isKiln();
            float from = kiln ? BrewhouseModel.KILN_DOOR.u0() : Math.max(BrewhouseModel.KETTLE_DOOR.u0(), 16 * cell.column());
            float to = kiln ? BrewhouseModel.KILN_DOOR.u1() : Math.min(BrewhouseModel.KETTLE_DOOR.u1(), 16 * cell.column() + 16);
            float u = from + random.nextFloat() * (to - from);
            float y = kiln ? 4 + random.nextFloat() * 3 : 4 + random.nextFloat() * 5;
            Vec3 at = BrewhouseShapes.worldPoint(pos, cell, facing, u, -BrewhouseModel.DOOR_DEPTH - 0.3F, y);
            level.addParticle(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 0, 0, 0);
            level.addParticle(ParticleTypes.SMOKE, at.x, at.y + 0.1, at.z, 0, 0.01, 0);
            if (random.nextDouble() < 0.25) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, SoundEvents.FURNACE_FIRE_CRACKLE,
                        SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }
        }
        if (cell.isKiln() && state.getValue(TRAY) == TrayLook.GREEN && random.nextDouble() < 0.15) {
            float u = BrewhouseModel.TRAY_PAN.u0() + random.nextFloat() * (BrewhouseModel.TRAY_PAN.u1() - BrewhouseModel.TRAY_PAN.u0());
            float v = 16 * cell.row() + 2 + random.nextFloat() * 12;
            Vec3 at = BrewhouseShapes.worldPoint(pos, cell, facing, u, v, BrewhouseModel.TRAY_GRATE.y1() + 0.2F);
            level.addParticle(ParticleTypes.WHITE_SMOKE, at.x, at.y, at.z, 0, 0.01, 0);
        }
    }
}
