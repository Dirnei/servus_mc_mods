package io.github.dirnei.mc.servus.logistics.datagen;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusLogisticsLanguageProvider extends LanguageProvider {

    ServusLogisticsLanguageProvider(PackOutput output) {
        super(output, ServusLogisticsIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ServusLogisticsBlocks.UTILITY_DUCT.get(), "Utility Duct");
        add(ServusLogisticsItems.ITEM_PIPE.get(), "Item Pipe");
        add(ServusLogisticsItems.POWER_CABLE.get(), "Power Cable");
        add(ServusLogisticsItems.FACADE.get(), "Facade");
        add("item.servus_logistics.facade.named", "Facade (%s)");
        add("item.servus_logistics.facade.hint", "Use on a utility duct to hide it");
        add(ServusLogisticsBlocks.FLUID_TANK.get(), "Fluid Tank");
        add("tooltip.servus_logistics.fluid_tank.empty", "Empty");
        add("tooltip.servus_logistics.fluid_tank.fluid", "%s: %s mB");
        add("message.servus_logistics.fluid_tank.empty", "Empty, capacity %s mB");
        add("message.servus_logistics.fluid_tank.content", "%s: %s / %s mB");
        add("message.servus_logistics.fluid_tank.mixed", "The tanks hold different fluids");
        add("gui.servus_logistics.utility_duct", "Utility Duct");
        add("gui.servus_logistics.utility_duct.title", "Utility Duct, %s: %s");
        add("gui.servus_logistics.remove.item", "Remove Item Pipe");
        add("gui.servus_logistics.remove.energy", "Remove Power Cable");
        add("gui.servus_logistics.filter", "Filter (empty = everything)");
        add("gui.servus_logistics.priority", "Priority %s");
        add("gui.servus_logistics.no_lines", "No lines");
        for (int channel = 1; channel <= 8; channel++) {
            add("gui.servus_logistics.channel." + channel, "Channel " + channel);
        }
        add("gui.servus_logistics.line.item", "Items");
        add("gui.servus_logistics.line.energy", "Energy");
        add("gui.servus_logistics.mode.both", "In and out");
        add("gui.servus_logistics.mode.auto", "In and out");
        add("gui.servus_logistics.mode.input", "Pull in");
        add("gui.servus_logistics.mode.output", "Push out");
        add("gui.servus_logistics.mode.disabled", "Disabled");
        add("gui.servus_logistics.side.down", "Down");
        add("gui.servus_logistics.side.up", "Up");
        add("gui.servus_logistics.side.north", "North");
        add("gui.servus_logistics.side.south", "South");
        add("gui.servus_logistics.side.west", "West");
        add("gui.servus_logistics.side.east", "East");
    }
}
