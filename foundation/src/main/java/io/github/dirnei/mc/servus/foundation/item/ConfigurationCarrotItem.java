package io.github.dirnei.mc.servus.foundation.item;

import io.github.dirnei.mc.servus.foundation.block.SideConfigWrench;
import io.github.dirnei.mc.servus.foundation.config.ServusFoundationConfig;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationComponents;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.HitResult;

/**
 * The Configuration Carrot, the item that fills the wrench role in Servus.
 * Blocks implementing {@link Wrenchable} get the click; everything else with
 * a facing property is rotated. The carrot carries a side layer (Items or
 * Fluids), switched by using it on the air, which decides which layer a
 * click on a block face configures and which layer the side markers show.
 */
public class ConfigurationCarrotItem extends Item {

    public ConfigurationCarrotItem(Properties properties) {
        super(properties);
    }

    /** The layer a carrot stack configures; Items for anything else. */
    public static SideLayer layer(ItemStack stack) {
        if (!(stack.getItem() instanceof ConfigurationCarrotItem)) {
            return SideLayer.ITEMS;
        }
        String name = stack.get(ServusFoundationComponents.CARROT_LAYER.get());
        return name == null ? SideLayer.ITEMS : SideLayer.byName(name);
    }

    /** The layer of the carrot the player holds in either hand, or null without one. */
    public static SideLayer heldLayer(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof ConfigurationCarrotItem) {
                return layer(stack);
            }
        }
        return null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof Wrenchable wrenchable) {
            return wrenchable.onWrench(context, state);
        }
        if (!ServusFoundationConfig.wrenchRotatesBlocks()) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        EnumProperty<Direction> property = facingProperty(state);
        if (property == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(property), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    /** Used on the air: switches to the next layer and says which. A click that reached a block does nothing here. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType() != HitResult.Type.MISS) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            SideLayer next = layer(stack).next();
            stack.set(ServusFoundationComponents.CARROT_LAYER.get(), next.name());
            player.sendOverlayMessage(Component.translatable("message.servus_foundation.carrot_layer",
                    SideConfigWrench.layerName(next)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("message.servus_foundation.carrot_layer", SideConfigWrench.layerName(layer(stack)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.servus_foundation.carrot_layer_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static EnumProperty<Direction> facingProperty(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return BlockStateProperties.HORIZONTAL_FACING;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return BlockStateProperties.FACING;
        }
        return null;
    }
}
