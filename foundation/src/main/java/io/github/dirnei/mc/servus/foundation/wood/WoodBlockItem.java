package io.github.dirnei.mc.servus.foundation.wood;

import java.util.List;
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.servus_foundation.wood", Wood.block(Wood.of(stack)).getName())
                .withStyle(ChatFormatting.GRAY));
    }
}
