package io.github.dirnei.mc.servus.shuttles.rail;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import io.github.dirnei.mc.servus.foundation.placement.AssistedBlockItem;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.servus_foundation.wood", Wood.block(Wood.of(stack)).getName())
                .withStyle(ChatFormatting.GRAY));
    }
}
