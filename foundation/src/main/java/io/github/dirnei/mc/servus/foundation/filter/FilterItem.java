package io.github.dirnei.mc.servus.foundation.filter;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A filter item that shows its wear from the filter data map as a durability bar and a tooltip line. */
public class FilterItem extends Item {

    public FilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        FilterSpec spec = FilterDataMaps.spec(stack);
        return spec != null && Filters.wearOf(stack, spec).used() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        FilterSpec spec = FilterDataMaps.spec(stack);
        return spec == null ? 0 : Filters.wearOf(stack, spec).barWidth(MAX_BAR_WIDTH);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        FilterSpec spec = FilterDataMaps.spec(stack);
        float share = spec == null ? 0 : (float) Filters.wearOf(stack, spec).left() / spec.points();
        return Mth.hsvToRgb(share / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        FilterSpec spec = FilterDataMaps.spec(stack);
        if (spec != null) {
            tooltip.accept(Component.translatable("tooltip.servus_foundation.filter.points",
                    Filters.wearOf(stack, spec).left(), spec.points()).withStyle(ChatFormatting.GRAY));
        }
    }
}
