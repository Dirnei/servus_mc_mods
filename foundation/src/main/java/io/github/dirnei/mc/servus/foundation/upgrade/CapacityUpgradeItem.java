package io.github.dirnei.mc.servus.foundation.upgrade;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Gives a block with capacity upgrade slots more room; the block decides how
 * much. Not a Foundation multiplier upgrade, so speed slots refuse it.
 */
public class CapacityUpgradeItem extends Item {

    public CapacityUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.servus_foundation.capacity_upgrade.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
