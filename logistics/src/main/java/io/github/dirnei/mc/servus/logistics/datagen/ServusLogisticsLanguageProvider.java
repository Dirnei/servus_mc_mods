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
        add(ServusLogisticsItems.ITEM_CHANNEL.get(), "Item Channel");
        add(ServusLogisticsItems.ENERGY_CHANNEL.get(), "Energy Channel");
        add("gui.servus_logistics.utility_duct", "Utility Duct");
        add("gui.servus_logistics.remove.item", "Remove item channel");
        add("gui.servus_logistics.remove.energy", "Remove energy channel");
        add("gui.servus_logistics.filter", "Filter (empty = everything)");
        add("gui.servus_logistics.priority", "Priority %s");
        add("gui.servus_logistics.no_channels", "No channels");
        add("gui.servus_logistics.channel.item", "Items");
        add("gui.servus_logistics.channel.energy", "Energy");
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
