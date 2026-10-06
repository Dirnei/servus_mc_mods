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
        add("message.servus_storage.zone_label.unnamed", "Give the label a name first: right click the air with it");
        add("message.servus_storage.zone_label.stamped", "Zone: %s");
        add("message.servus_storage.zone_label.cleared", "Zone cleared");
        add("gui.servus_storage.search", "Search");
        add("gui.servus_storage.stored", "Stored: %s");
        add("gui.servus_storage.terminal.not_connected", "Not connected to a warehouse");
        add("gui.servus_storage.terminal.no_controller", "Warehouse has no controller");
        add("gui.servus_storage.terminal.multiple_controllers", "Warehouse has more than one controller");
        add("gui.servus_storage.station.zones", "Zones");
        add("gui.servus_storage.station.no_zones", "No zones yet: define a Zone Label and stamp storage blocks");
        add("gui.servus_storage.station.blocks", "%s blocks");
        add("gui.servus_storage.station.items", "Items: %s");
        add("gui.servus_storage.station.tags", "Tags: %s");
        add("gui.servus_storage.station.takes_all", "Takes any item");
        add("gui.servus_storage.station.idle", "Idle");
        add("gui.servus_storage.station.busy", "Putting away");
        add("gui.servus_storage.station.blocked.no_room", "%s: no room in %s or in general storage");
        add("gui.servus_storage.station.blocked.no_room_general", "%s: no room in general storage");
        add("gui.servus_storage.station.blocked.no_storage", "%s: no storage to go to");
        add("gui.servus_storage.zone_label.title", "Zone Label");
        add("gui.servus_storage.zone_label.name", "Zone name");
        add("gui.servus_storage.zone_label.name_hint", "zone name");
        add("gui.servus_storage.zone_label.items", "Items this zone takes");
        add("gui.servus_storage.zone_label.tags", "Tags");
        add("gui.servus_storage.zone_label.tags_hint", "tags, comma separated, for example c:ores");
    }
}
