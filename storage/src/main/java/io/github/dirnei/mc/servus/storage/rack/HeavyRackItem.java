package io.github.dirnei.mc.servus.storage.rack;

import java.util.List;
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Block planks = BuiltInRegistries.BLOCK.get(HeavyRackBlockEntity.woodOf(stack));
        tooltip.add(Component.translatable("tooltip.servus_storage.heavy_rack.wood", planks.getName())
                .withStyle(ChatFormatting.GRAY));
    }
}
