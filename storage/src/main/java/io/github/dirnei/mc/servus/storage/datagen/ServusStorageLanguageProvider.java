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
        add(ServusStorageBlocks.DISPATCH_STATION.get(), "Dispatch Station");
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
        add("gui.servus_storage.station.zones", "Zones");
        add("gui.servus_storage.station.no_zones", "No zones yet: stamp storage blocks with a Zone Label");
        add("gui.servus_storage.station.blocks", "%s blocks");
        add("gui.servus_storage.station.rule", "Rule for %s");
        add("gui.servus_storage.station.no_zone", "Select a zone to edit its rule");
        add("gui.servus_storage.station.tags", "Tags");
        add("gui.servus_storage.station.tags_hint", "tags, comma separated");
        add("gui.servus_storage.station.overflow_off", "Overflow");
        add("gui.servus_storage.station.overflow_on", "Overflow *");
        add("gui.servus_storage.station.idle", "Idle");
        add("gui.servus_storage.station.busy", "Putting away");
        add("gui.servus_storage.station.general", "general storage");
        add("gui.servus_storage.station.blocked.zone_full_no_overflow", "%s: no room in %s, no overflow zone");
        add("gui.servus_storage.station.blocked.overflow_full", "%s: no room in %s, overflow zone full");
        add("gui.servus_storage.station.blocked.no_storage", "%s: no storage to go to");
    }
}
