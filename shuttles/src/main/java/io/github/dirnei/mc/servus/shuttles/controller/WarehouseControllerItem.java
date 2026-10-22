package io.github.dirnei.mc.servus.shuttles.controller;

import io.github.dirnei.mc.servus.foundation.material.MaterialBlockItem;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The Warehouse Controller item; its tooltip names the site and the FE it carries. */
public class WarehouseControllerItem extends MaterialBlockItem {

    public WarehouseControllerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        carriedLines(stack).forEach(tooltip);
    }

    /** The site name and FE lines of a dropped controller; empty for a fresh one. */
    public static List<Component> carriedLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        String name = stack.get(ServusShuttlesComponents.SITE_NAME.get());
        if (name != null && !name.isEmpty()) {
            lines.add(Component.translatable("tooltip.servus_shuttles.warehouse_controller.site", name).withStyle(ChatFormatting.GRAY));
        }
        Integer energy = stack.get(ServusShuttlesComponents.CONTROLLER_ENERGY.get());
        if (energy != null && energy > 0) {
            lines.add(Component.translatable("tooltip.servus_shuttles.warehouse_controller.energy", ControllerEnergy.format(energy))
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
