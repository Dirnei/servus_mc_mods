package io.github.dirnei.mc.servus.foundation.wood;

import net.minecraft.world.item.component.TooltipDisplay;
import io.github.dirnei.mc.servus.foundation.block.TooltipBlock;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** The item of a wood block; its tooltip names the planks it was crafted from. */
public class WoodBlockItem extends BlockItem {

    public WoodBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (getBlock() instanceof TooltipBlock block) {
            block.appendTooltip(stack, context, display, tooltip, flag);
        }
        tooltip.accept(Component.translatable("tooltip.servus_foundation.wood", Wood.block(Wood.of(stack)).getName())
                .withStyle(ChatFormatting.GRAY));
    }
}
