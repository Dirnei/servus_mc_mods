package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusStorageLanguageProvider extends LanguageProvider {

    ServusStorageLanguageProvider(PackOutput output) {
        super(output, ServusStorageIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ServusStorageBlocks.CRATE.get(), "Crate");
        add(ServusStorageBlocks.RACK.get(), "Shelving Rack");
        add(ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(), "Warehouse Controller");
        add(ServusStorageBlocks.COUNTER_TERMINAL.get(), "Counter Terminal");
        add(ServusStorageBlocks.RAIL.get(), "Warehouse Rail");
        add(ServusStorageBlocks.ELEVATOR.get(), "Rail Elevator");
        add("gui.servus_storage.search", "Search");
        add("gui.servus_storage.stored", "Stored: %s");
        add("gui.servus_storage.terminal.not_connected", "Not connected to a warehouse");
        add("gui.servus_storage.terminal.no_controller", "Warehouse has no controller");
        add("gui.servus_storage.terminal.multiple_controllers", "Warehouse has more than one controller");
    }
}
