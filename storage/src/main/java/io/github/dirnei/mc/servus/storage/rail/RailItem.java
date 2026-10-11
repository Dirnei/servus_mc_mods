package io.github.dirnei.mc.servus.storage.rail;

import io.github.dirnei.mc.servus.foundation.placement.AssistedBlockItem;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** A rail item: sneak placement extends a run, and the tooltip names the wood of its sleepers. */
public class RailItem extends AssistedBlockItem {

    public RailItem(Block block, Properties properties) {
        super(block, RailAssist.INSTANCE, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.servus_foundation.wood", Wood.block(Wood.of(stack)).getName())
                .withStyle(ChatFormatting.GRAY));
    }
}
