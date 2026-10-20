package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.logistics.datagen.LogisticsDataParts;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusFoundationLanguageProvider extends LanguageProvider {

    ServusFoundationLanguageProvider(PackOutput output) {
        super(output, ServusFoundationIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("tooltip.servus_foundation.wood", "Wood: %s");
        add(ServusFoundationItems.CONFIGURATION_CARROT.get(), "Configuration Carrot");
        add(ServusFoundationItems.DATA_POTATO.get(), "Data Potato");
        add("tooltip.servus_foundation.data_potato", "Hold to see data cable load; use on a duct or tank for a report");
        add(ServusFoundationItems.GOGGLES.get(), "Engineer Goggles");
        add(AdapterContent.BLOCK.get(), "Machine Adapter");
        MachineDataParts.lang(this);
        add("gui.servus_foundation.picker.search", "Search");
        add("gui.servus_foundation.machine_adapter.machine", "Machine: %s");
        add("gui.servus_foundation.machine_adapter.no_profile", "No machine this adapter knows");
        add("gui.servus_foundation.machine_adapter.upkeep", "Upkeep item and level");
        add("gui.servus_foundation.machine_adapter.count", "%s of %s in the machine");
        add("gui.servus_foundation.machine_adapter.no_upkeep", "No upkeep");
        add(ServusFoundationItems.SPEED_UPGRADE.get(), "Speed Upgrade");
        add("tooltip.servus_foundation.upgrade.speed", "Speed x%s in upgrade slots");
        add(ServusFoundationItems.CAPACITY_UPGRADE.get(), "Capacity Upgrade");
        add("item.servus_foundation.capacity_upgrade.tooltip", "Gives a block more room in capacity upgrade slots");
        add("message.servus_foundation.data_cable.summary", "Data network: %s cables, %s producers, %s stock");
        add("message.servus_foundation.data_cable.summary_load", "Data network: %s cables, %s producers, %s stock; this duct %s of %s items/s");
        add("commands.servus_foundation.data.move.started", "Delivery %s started: %s %s");
        add("commands.servus_foundation.data.move.failed", "Delivery %s failed: %s");
        add("commands.servus_foundation.data.move.invalid", "Nothing to move");
        add("itemGroup.servus", "Servus");
        add("servus_foundation.configuration.wrench", "Configuration Carrot");
        add("servus_foundation.configuration.wrenchRotatesBlocks", "Configuration Carrot rotates blocks");
        add("gui.servus_foundation.side.front", "Front");
        add("gui.servus_foundation.side.back", "Back");
        add("gui.servus_foundation.side.left", "Left");
        add("gui.servus_foundation.side.right", "Right");
        add("gui.servus_foundation.side.top", "Top");
        add("gui.servus_foundation.side.bottom", "Bottom");
        add("gui.servus_foundation.side_mode.auto", "Auto");
        add("gui.servus_foundation.side_mode.input", "Input");
        add("gui.servus_foundation.side_mode.output", "Output");
        add("gui.servus_foundation.side_mode.both", "Input and Output");
        add("gui.servus_foundation.side_mode.pull", "Pull");
        add("gui.servus_foundation.side_mode.push", "Push");
        add("gui.servus_foundation.side_mode.off", "Off");
        add("gui.servus_foundation.side_line", "%s: %s");
        add("gui.servus_foundation.side_layer.items", "Items");
        add("gui.servus_foundation.side_layer.fluids", "Fluids");
        add("gui.servus_foundation.fluid_side.all", "All fluids");
        add("gui.servus_foundation.side_tab", "%s sides");
        add("message.servus_foundation.carrot_layer", "Layer: %s");
        add("tooltip.servus_foundation.carrot_layer_hint", "Use on the air to switch the layer");
        add("gui.servus_foundation.side_fixed", "(fixed)");
        add("gui.servus_foundation.fluid_gauge.empty", "Empty");
        LogisticsDataParts.language(this);
    }
}
