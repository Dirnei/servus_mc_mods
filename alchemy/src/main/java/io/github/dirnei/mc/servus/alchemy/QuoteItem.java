package io.github.dirnei.mc.servus.alchemy;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A plain item whose tooltip carries a line in Paracelsus' voice, from the language key {@code <item key>.quote}. */
public class QuoteItem extends Item {

    public QuoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable(getDescriptionId() + ".quote").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
