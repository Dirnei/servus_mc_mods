package io.github.dirnei.mc.servus.foundation.upgrade;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Widens an Ender Anchor's area by one ring of chunks and lets a warehouse
 * site wake. Not a Foundation multiplier upgrade.
 */
public class AnchorUpgradeItem extends Item {

    public AnchorUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.servus_foundation.anchor_upgrade.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
