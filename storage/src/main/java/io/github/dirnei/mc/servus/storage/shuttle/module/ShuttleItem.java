package io.github.dirnei.mc.servus.storage.shuttle.module;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A shuttle for a Shuttle Garage; its tooltip lists the modules the workshop fitted. */
public class ShuttleItem extends Item {

    public ShuttleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ShuttleModules modules = ShuttleModules.of(stack);
        if (modules.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.servus_storage.shuttle.plain").withStyle(ChatFormatting.GRAY));
            return;
        }
        line(tooltip, "tooltip.servus_storage.shuttle.tool", modules.tool());
        line(tooltip, "tooltip.servus_storage.shuttle.utility", modules.utility());
    }

    private static void line(List<Component> tooltip, String key, Optional<Item> module) {
        module.ifPresent(item -> tooltip.add(Component.translatable(key, item.getDescription()).withStyle(ChatFormatting.GRAY)));
    }
}
