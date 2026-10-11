package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.foundation.placement.AssistedBlockItem;
import io.github.dirnei.mc.servus.foundation.wood.WoodBlockItem;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackItem;
import io.github.dirnei.mc.servus.storage.rail.ElevatorItem;
import io.github.dirnei.mc.servus.storage.rail.RailAssist;
import io.github.dirnei.mc.servus.storage.shuttle.module.ModuleSlot;
import io.github.dirnei.mc.servus.storage.shuttle.module.ModuleSpec;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModuleItem;
import io.github.dirnei.mc.servus.storage.zone.ZoneLabelItem;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusStorageIds.MOD_ID);

    public static final DeferredItem<BlockItem> CRATE = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.CRATE);
    public static final DeferredItem<BlockItem> DOUBLE_CRATE = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.DOUBLE_CRATE);
    public static final DeferredItem<BlockItem> QUAD_CRATE = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.QUAD_CRATE);
    public static final DeferredItem<HeavyRackItem> HEAVY_RACK = ITEMS.registerItem("heavy_rack",
            properties -> new HeavyRackItem(ServusStorageBlocks.HEAVY_RACK.get(), properties));
    public static final DeferredItem<WoodBlockItem> WAREHOUSE_CONTROLLER = ITEMS.registerItem("warehouse_controller",
            properties -> new WoodBlockItem(ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(), properties));
    public static final DeferredItem<WoodBlockItem> STORAGE_TERMINAL = ITEMS.registerItem("storage_terminal",
            properties -> new WoodBlockItem(ServusStorageBlocks.STORAGE_TERMINAL.get(), properties));
    public static final DeferredItem<WoodBlockItem> PRODUCTION_TERMINAL = ITEMS.registerItem("production_terminal",
            properties -> new WoodBlockItem(ServusStorageBlocks.PRODUCTION_TERMINAL.get(), properties));
    public static final DeferredItem<WoodBlockItem> PATTERN_STORAGE = ITEMS.registerItem("pattern_storage",
            properties -> new WoodBlockItem(ServusStorageBlocks.PATTERN_STORAGE.get(), properties));
    public static final DeferredItem<AssistedBlockItem> RAIL = ITEMS.registerItem("rail",
            properties -> new AssistedBlockItem(ServusStorageBlocks.RAIL.get(), RailAssist.INSTANCE, properties));
    public static final DeferredItem<AssistedBlockItem> FARM_RAIL = ITEMS.registerItem("farm_rail",
            properties -> new AssistedBlockItem(ServusStorageBlocks.FARM_RAIL.get(), RailAssist.INSTANCE, properties));
    public static final DeferredItem<ElevatorItem> ELEVATOR = ITEMS.registerItem("elevator",
            properties -> new ElevatorItem(ServusStorageBlocks.ELEVATOR.get(), properties));
    public static final DeferredItem<WoodBlockItem> INBOUND_STATION = ITEMS.registerItem("inbound_station",
            properties -> new WoodBlockItem(ServusStorageBlocks.INBOUND_STATION.get(), properties));
    public static final DeferredItem<WoodBlockItem> PACKING_STATION = ITEMS.registerItem("packing_station",
            properties -> new WoodBlockItem(ServusStorageBlocks.PACKING_STATION.get(), properties));
    public static final DeferredItem<WoodBlockItem> SHUTTLE_GARAGE = ITEMS.registerItem("shuttle_garage",
            properties -> new WoodBlockItem(ServusStorageBlocks.SHUTTLE_GARAGE.get(), properties));
    public static final DeferredItem<WoodBlockItem> OUTBOUND_STATION = ITEMS.registerItem("outbound_station",
            properties -> new WoodBlockItem(ServusStorageBlocks.OUTBOUND_STATION.get(), properties));
    public static final DeferredItem<WoodBlockItem> CRAFTING_STATION = ITEMS.registerItem("crafting_station",
            properties -> new WoodBlockItem(ServusStorageBlocks.CRAFTING_STATION.get(), properties));
    public static final DeferredItem<WoodBlockItem> CRAFTING_CPU = ITEMS.registerItem("crafting_cpu",
            properties -> new WoodBlockItem(ServusStorageBlocks.CRAFTING_CPU.get(), properties));
    public static final DeferredItem<BlockItem> SHUTTLE_WORKSHOP = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.SHUTTLE_WORKSHOP);
    public static final DeferredItem<ShuttleItem> SHUTTLE = ITEMS.registerItem("shuttle", ShuttleItem::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<ShuttleModuleItem> CARGO_HOLD = ITEMS.registerItem("cargo_hold",
            properties -> new ShuttleModuleItem(new ModuleSpec("cargo_hold", ModuleSlot.UTILITY, false, true), properties),
            new Item.Properties().stacksTo(16));
    public static final DeferredItem<ZoneLabelItem> ZONE_LABEL = ITEMS.registerItem("zone_label", ZoneLabelItem::new, new Item.Properties().stacksTo(1));

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(CRATE, DOUBLE_CRATE, QUAD_CRATE, HEAVY_RACK, WAREHOUSE_CONTROLLER, STORAGE_TERMINAL, RAIL, ELEVATOR, INBOUND_STATION, PACKING_STATION, SHUTTLE_GARAGE, OUTBOUND_STATION, CRAFTING_STATION, CRAFTING_CPU, PRODUCTION_TERMINAL, PATTERN_STORAGE, SHUTTLE_WORKSHOP, SHUTTLE, CARGO_HOLD, FARM_RAIL, ZONE_LABEL);

    private ServusStorageItems() {
    }
}
