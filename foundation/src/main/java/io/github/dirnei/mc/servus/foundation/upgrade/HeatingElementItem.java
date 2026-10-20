package io.github.dirnei.mc.servus.foundation.upgrade;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Lets a firebox run on FE instead of fuel; the mod that owns the firebox decides how. Neither a speed upgrade nor a
 * Capacity Upgrade, so upgrade and capacity slots refuse it.
 */
public class HeatingElementItem extends Item {

    public HeatingElementItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.servus_foundation.heating_element.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
