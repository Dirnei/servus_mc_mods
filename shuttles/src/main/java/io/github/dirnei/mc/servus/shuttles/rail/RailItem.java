package io.github.dirnei.mc.servus.shuttles.rail;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import io.github.dirnei.mc.servus.foundation.placement.AssistedBlockItem;
import io.github.dirnei.mc.servus.foundation.material.Material;
import io.github.dirnei.mc.servus.foundation.material.MaterialBlockItem;
import io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * A rail item: sneak placement extends a run, and the tooltip names the wood of
 * its sleepers and, past fresh copper, the corrosion stage of a farm rail.
 */
public class RailItem extends AssistedBlockItem {

    public RailItem(Block block, Properties properties) {
        super(block, RailAssist.INSTANCE, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(MaterialBlockItem.line(stack, Material.kindOf(stack)));
        RailCorrosion corrosion = stack.get(ServusShuttlesComponents.RAIL_CORROSION.get());
        if (corrosion != null && corrosion.stage() > FieldCorrosion.COPPER) {
            tooltip.accept(Component.translatable("tooltip.servus_shuttles.farm_rail.stage",
                    Component.translatable("gui.servus_shuttles.field_head.stage." + FieldCorrosion.name(corrosion.stage())))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
