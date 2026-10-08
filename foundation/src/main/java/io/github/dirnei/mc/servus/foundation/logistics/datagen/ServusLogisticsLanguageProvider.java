package io.github.dirnei.mc.servus.foundation.logistics.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusLogisticsLanguageProvider extends LanguageProvider {

    ServusLogisticsLanguageProvider(PackOutput output) {
        super(output, ServusFoundationIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ServusLogisticsBlocks.UTILITY_DUCT.get(), "Utility Duct");
        add(ServusLogisticsItems.ITEM_PIPE.get(), "Item Pipe");
        add(ServusLogisticsItems.POWER_CABLE.get(), "Power Cable");
        add(ServusLogisticsItems.FLUID_PIPE.get(), "Fluid Pipe");
        add(ServusLogisticsItems.FACADE.get(), "Facade");
        add("item.servus_foundation.facade.named", "Facade (%s)");
        add("item.servus_foundation.facade.hint", "Use on a utility duct to hide it");
        add(ServusLogisticsBlocks.FLUID_TANK.get(), "Fluid Tank");
        add("tooltip.servus_foundation.fluid_tank.empty", "Empty");
        add("tooltip.servus_foundation.fluid_tank.fluid", "%s: %s mB");
        add("message.servus_foundation.fluid_tank.empty", "Empty, capacity %s mB");
        add("message.servus_foundation.fluid_tank.content", "%s: %s / %s mB");
        add("message.servus_foundation.fluid_tank.mixed", "The tanks hold different fluids");
        add("gui.servus_foundation.utility_duct", "Utility Duct");
        add("gui.servus_foundation.utility_duct.title", "Utility Duct, %s: %s");
        add("gui.servus_foundation.remove.item", "Remove Item Pipe");
        add("gui.servus_foundation.remove.energy", "Remove Power Cable");
        add("gui.servus_foundation.remove.fluid", "Remove Fluid Pipe");
        add("gui.servus_foundation.filter", "Filter (empty = everything)");
        add("gui.servus_foundation.priority", "Priority %s");
        add("gui.servus_foundation.no_lines", "No lines");
        for (int channel = 1; channel <= 8; channel++) {
            add("gui.servus_foundation.channel." + channel, "Channel " + channel);
        }
        add("gui.servus_foundation.line.item", "Items");
        add("gui.servus_foundation.line.energy", "Energy");
        add("gui.servus_foundation.line.fluid", "Fluids");
        add("gui.servus_foundation.mode.both", "In and out");
        add("gui.servus_foundation.mode.auto", "In and out");
        add("gui.servus_foundation.mode.input", "Pull in");
        add("gui.servus_foundation.mode.output", "Push out");
        add("gui.servus_foundation.mode.disabled", "Disabled");
        add("gui.servus_foundation.side.down", "Down");
        add("gui.servus_foundation.side.up", "Up");
        add("gui.servus_foundation.side.north", "North");
        add("gui.servus_foundation.side.south", "South");
        add("gui.servus_foundation.side.west", "West");
        add("gui.servus_foundation.side.east", "East");
    }
}
