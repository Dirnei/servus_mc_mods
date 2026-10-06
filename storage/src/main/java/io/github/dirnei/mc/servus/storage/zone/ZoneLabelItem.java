package io.github.dirnei.mc.servus.storage.zone;

import io.github.dirnei.mc.servus.storage.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A stamp for zones: right click the air to name it and set its filter, right
 * click storage blocks to copy that onto them, sneak right click to clear.
 * Never consumed.
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
        ZoneDefinition definition = ZoneDefinition.of(stack);
        if (!clear && definition == null) {
            if (!level.isClientSide() && player != null) {
                player.displayClientMessage(Component.translatable("message.servus_storage.zone_label.unnamed"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide()) {
            node.setZone(clear ? null : definition);
            if (player != null) {
                Component message = node.zone().isEmpty()
                        ? Component.translatable("message.servus_storage.zone_label.cleared")
                        : Component.translatable("message.servus_storage.zone_label.stamped", node.zone());
                player.displayClientMessage(message, true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Right click in the air, or on a block that takes no zone: open the label's GUI. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ZoneLabelMenu(id, inventory, hand),
                    Component.translatable("gui.servus_storage.zone_label.title")), buf -> buf.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
