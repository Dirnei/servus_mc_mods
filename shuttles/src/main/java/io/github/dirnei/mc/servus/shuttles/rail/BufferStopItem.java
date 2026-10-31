package io.github.dirnei.mc.servus.shuttles.rail;

import io.github.dirnei.mc.servus.foundation.material.MaterialBlockItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The Buffer Stop item: the tooltip names its wood and the rail it was crafted from. */
public class BufferStopItem extends MaterialBlockItem {

    public BufferStopItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.servus_shuttles.buffer_stop.rail",
                Component.translatable("block.servus_shuttles." + (BufferStopBlock.railOf(stack) == BufferStopBlock.Rail.FARM ? "farm_rail" : "rail")))
                .withStyle(ChatFormatting.GRAY));
    }
}
