package io.github.dirnei.mc.servus.storage.controller;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Goes into a Warehouse Controller and lets its site wake for transfers. Not a Foundation multiplier upgrade. */
public class ChunkLoaderUpgradeItem extends Item {

    public ChunkLoaderUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.servus_storage.chunk_loader_upgrade.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
