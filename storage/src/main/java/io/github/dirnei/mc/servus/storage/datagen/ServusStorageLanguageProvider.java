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
        add(ServusStorageBlocks.INBOUND_STATION.get(), "Inbound Station");
        add(ServusStorageBlocks.SHUTTLE_GARAGE.get(), "Shuttle Garage");
        add(ServusStorageBlocks.MACHINE_DOCK.get(), "Machine Dock");
        add(ServusStorageBlocks.CRAFTING_STATION.get(), "Crafting Station");
        add("gui.servus_storage.crafting_station.save", "Save");
        add("gui.servus_storage.crafting_station.recipes", "Recipes");
        add("gui.servus_storage.crafting_station.remove_hint", "Click to remove");
        add("gui.servus_storage.crafting_station.progress", "Crafting %s%%");
        add("gui.servus_storage.crafting_station.message.saved", "Recipe saved");
        add("gui.servus_storage.crafting_station.message.empty", "Lay out a recipe in the grid first");
        add("gui.servus_storage.crafting_station.message.no_match", "The grid matches no recipe");
        add("gui.servus_storage.crafting_station.message.duplicate", "That recipe is already configured");
        add("gui.servus_storage.crafting_station.message.full", "All recipe slots are in use");
        add("gui.servus_storage.dock.feed", "To machine");
        add("gui.servus_storage.dock.collect", "From machine");
        add("message.servus_storage.garage.occupied", "Take the shuttles out of the garage first");
        add("gui.servus_storage.garage.status", "%s of %s shuttles, %s out");
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
        add("gui.servus_storage.terminal.requests", "Requests");
        add("gui.servus_storage.terminal.output", "Output");
        add("gui.servus_storage.terminal.amount", "Amount");
        add("gui.servus_storage.terminal.plus_stack", "+Stack");
        add("gui.servus_storage.terminal.request", "Request");
        add("gui.servus_storage.terminal.cancel", "Cancel");
        add("gui.servus_storage.terminal.click_hint", "Click to request, shift click for a stack");
        add("gui.servus_storage.terminal.cancel_hint", "Click to cancel this request");
        add("gui.servus_storage.terminal.no_requests", "No open requests");
        add("gui.servus_storage.terminal.state.waiting_shuttle", "waiting for a shuttle");
        add("gui.servus_storage.terminal.state.picking", "picking");
        add("gui.servus_storage.terminal.state.waiting_room", "waiting for room");
        add("gui.servus_storage.terminal.state.producing", "producing");
        add("gui.servus_storage.terminal.state.missing", "%s missing");
        add("gui.servus_storage.terminal.message.offline", "The warehouse is offline");
        add("gui.servus_storage.terminal.message.needs_shuttle", "Only racks hold this and no shuttle can reach them");
        add("gui.servus_storage.terminal.message.no_rail", "This terminal has no rail for shuttle deliveries");
        add("gui.servus_storage.terminal.message.out_of_stock", "Nothing of that in stock");
        add("gui.servus_storage.station.blocked.no_room", "%s: no room in %s or in general storage");
        add("gui.servus_storage.station.blocked.no_room_general", "%s: no room in general storage");
        add("gui.servus_storage.station.blocked.no_storage", "%s: no storage to go to");
        add("gui.servus_storage.station.blocked.no_shuttle", "%s: only racks have room and no shuttle can reach them");
        add("gui.servus_storage.zone_label.title", "Zone Label");
        add("gui.servus_storage.zone_label.name", "Zone name");
        add("gui.servus_storage.zone_label.name_hint", "zone name");
        add("gui.servus_storage.zone_label.items", "Items this zone takes");
        add("gui.servus_storage.zone_label.tags", "Tags");
        add("gui.servus_storage.zone_label.tags_hint", "tags, comma separated, for example c:ores");
    }
}
