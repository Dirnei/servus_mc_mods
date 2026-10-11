package io.github.dirnei.mc.servus.foundation.block;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A block that adds lines to its item's tooltip. Blocks lost their tooltip
 * hook in 1.21.5; the Servus block items ask blocks implementing this instead.
 */
public interface TooltipBlock {

    void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag);
}
