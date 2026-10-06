package io.github.dirnei.mc.servus.storage.zone;

import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A stamp for zones: name it in an anvil, right click storage blocks to put
 * them into that zone, sneak right click to clear. Never consumed.
 */
public class ZoneLabelItem extends Item {

    public ZoneLabelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof StorageNodeBlockEntity node) || !node.zonable()) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        boolean clear = player != null && player.isShiftKeyDown();
        if (!clear && !stack.has(DataComponents.CUSTOM_NAME)) {
            if (!level.isClientSide() && player != null) {
                player.displayClientMessage(Component.translatable("message.servus_storage.zone_label.unnamed"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide()) {
            String name = clear ? "" : stack.getHoverName().getString();
            node.setZone(name);
            if (player != null) {
                Component message = node.zone().isEmpty()
                        ? Component.translatable("message.servus_storage.zone_label.cleared")
                        : Component.translatable("message.servus_storage.zone_label.stamped", node.zone());
                player.displayClientMessage(message, true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
