package io.github.dirnei.mc.servus.shuttles.controller;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Goes into a Warehouse Controller and makes it the site's antenna for wireless transfers. Not a Foundation multiplier upgrade. */
public class WirelessUpgradeItem extends Item {

    public WirelessUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.servus_shuttles.wireless_upgrade.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
