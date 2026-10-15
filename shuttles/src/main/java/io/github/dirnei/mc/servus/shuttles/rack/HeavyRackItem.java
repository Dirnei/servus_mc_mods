package io.github.dirnei.mc.servus.shuttles.rack;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** The rack item; its tooltip names the planks it was crafted from. */
public class HeavyRackItem extends BlockItem {

    public HeavyRackItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        Block planks = BuiltInRegistries.BLOCK.getValue(HeavyRackBlockEntity.woodOf(stack));
        tooltip.accept(Component.translatable("tooltip.servus_shuttles.heavy_rack.wood", planks.getName())
                .withStyle(ChatFormatting.GRAY));
    }
}
