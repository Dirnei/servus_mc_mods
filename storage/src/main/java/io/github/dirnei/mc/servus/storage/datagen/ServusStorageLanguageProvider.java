package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusStorageLanguageProvider extends LanguageProvider {

    ServusStorageLanguageProvider(PackOutput output) {
        super(output, ServusStorageIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ServusStorageBlocks.CRATE.get(), "Crate");
        add(ServusStorageBlocks.DOUBLE_CRATE.get(), "Double Crate");
        add(ServusStorageBlocks.QUAD_CRATE.get(), "Quad Crate");
        add(ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(), "Warehouse Controller");
        add(ServusStorageBlocks.COUNTER_TERMINAL.get(), "Counter Terminal");
        add(ServusStorageBlocks.RAIL.get(), "Warehouse Rail");
        add(ServusStorageBlocks.ELEVATOR.get(), "Rail Elevator");
        add(ServusStorageBlocks.HEAVY_RACK.get(), "Heavy Duty Rack");
        add(ServusStorageItems.SHUTTLE.get(), "Shuttle");
        add(ServusStorageItems.ZONE_LABEL.get(), "Zone Label");
        add("message.servus_storage.zone_label.unnamed", "Name the label in an anvil first");
        add("message.servus_storage.zone_label.stamped", "Zone: %s");
        add("message.servus_storage.zone_label.cleared", "Zone cleared");
        add("gui.servus_storage.search", "Search");
        add("gui.servus_storage.stored", "Stored: %s");
        add("gui.servus_storage.terminal.not_connected", "Not connected to a warehouse");
        add("gui.servus_storage.terminal.no_controller", "Warehouse has no controller");
        add("gui.servus_storage.terminal.multiple_controllers", "Warehouse has more than one controller");
    }
}
