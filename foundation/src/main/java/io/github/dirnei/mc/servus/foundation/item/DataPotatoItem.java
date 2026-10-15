package io.github.dirnei.mc.servus.foundation.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Data Potato, Servus' investigation tool. Held, it shows analysis views
 * such as the data duct load; it never configures or dismantles anything.
 */
public class DataPotatoItem extends Item {

    public DataPotatoItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.servus_foundation.data_potato").withStyle(ChatFormatting.GRAY));
    }
}
