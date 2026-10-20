package io.github.dirnei.mc.servus.brewery.brewhouse;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Kind;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A part of the Brewhouse: loose a full block, formed its cell of the one big model. The carrot forms the Brewhouse
 * through a loose part, tells a formed part's state, and dismantles a part when sneaking.
 */
public abstract class BrewhousePartBlock extends BaseEntityBlock implements Wrenchable {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** The Brewhouse's front, where the fire doors are. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected BrewhousePartBlock(Properties properties) {
        super(properties);
    }

    /** Which block of the layout this is. */
    public abstract Kind kind();

    /** The part a formed state of this block takes. */
    public abstract BrewhousePart part(BlockState state);

    /** What a carrot click tells on a formed part of this kind, or does on it. */
    protected abstract Component wrenchFormed(BrewhouseBlockEntity part, BlockState state);

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewhouseBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? BrewhouseShapes.shape(part(state).cell(), state.getValue(FACING)) : Shapes.block();
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(FORMED) ? PushReaction.BLOCK : null;
    }

    /** Carrot: forms the Brewhouse through a loose part, or works on a formed one. Sneak carrot: dismantles the part. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player != null && player.isShiftKeyDown()) {
            Block.popResource(level, pos, new ItemStack(this));
            level.removeBlock(pos, false);
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof BrewhouseBlockEntity part)) {
            return InteractionResult.PASS;
        }
        Component message;
        if (state.getValue(FORMED)) {
            if (part.master() == null) {
                part.unformStray();
                message = Component.translatable("message.servus_brewery.brewhouse.loose");
            } else {
                message = wrenchFormed(part, state);
            }
        } else {
            Direction face = context.getClickedFace();
            Direction front = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
            message = BrewhouseForming.tryForm(level, pos, front)
                    ? Component.translatable("message.servus_brewery.brewhouse.formed")
                    : Component.translatable("message.servus_brewery.brewhouse.layout");
        }
        if (player != null) {
            player.sendOverlayMessage(message);
        }
        return InteractionResult.SUCCESS;
    }

    /** Whether {@code stack} is the Foundation carrot, which works through {@link #onWrench}. */
    protected static boolean isCarrot(ItemStack stack) {
        return stack.getItem() instanceof ConfigurationCarrotItem;
    }

    /** The formed part at {@code pos} with a valid master, or null. */
    protected static @Nullable BrewhouseBlockEntity formedPart(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(FORMED) || !(level.getBlockEntity(pos) instanceof BrewhouseBlockEntity part) || part.master() == null) {
            return null;
        }
        return part;
    }
}
