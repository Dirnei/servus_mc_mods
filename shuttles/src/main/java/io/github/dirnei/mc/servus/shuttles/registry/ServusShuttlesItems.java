package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.foundation.material.MaterialBlockItem;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerItem;
import io.github.dirnei.mc.servus.shuttles.controller.WirelessUpgradeItem;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationItem;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackItem;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackItem;
import io.github.dirnei.mc.servus.shuttles.rail.ElevatorItem;
import io.github.dirnei.mc.servus.shuttles.rail.RailItem;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ModuleSpec;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModuleItem;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldItem;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusShuttlesItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusShuttlesIds.MOD_ID);

    public static final DeferredItem<HeavyRackItem> HEAVY_RACK = ITEMS.registerItem("heavy_rack",
            properties -> new HeavyRackItem(ServusShuttlesBlocks.HEAVY_RACK.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<FluidRackItem> FLUID_RACK = ITEMS.registerItem("fluid_rack",
            properties -> new FluidRackItem(ServusShuttlesBlocks.FLUID_RACK.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WarehouseControllerItem> WAREHOUSE_CONTROLLER = ITEMS.registerItem("warehouse_controller",
            properties -> new WarehouseControllerItem(ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<MaterialBlockItem> STORAGE_TERMINAL = ITEMS.registerItem("storage_terminal",
            properties -> new MaterialBlockItem(ServusShuttlesBlocks.STORAGE_TERMINAL.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<MaterialBlockItem> PRODUCTION_TERMINAL = ITEMS.registerItem("production_terminal",
            properties -> new MaterialBlockItem(ServusShuttlesBlocks.PRODUCTION_TERMINAL.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<MaterialBlockItem> PATTERN_STORAGE = ITEMS.registerItem("pattern_storage",
            properties -> new MaterialBlockItem(ServusShuttlesBlocks.PATTERN_STORAGE.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<RailItem> RAIL = ITEMS.registerItem("rail",
            properties -> new RailItem(ServusShuttlesBlocks.RAIL.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<RailItem> FARM_RAIL = ITEMS.registerItem("farm_rail",
            properties -> new RailItem(ServusShuttlesBlocks.FARM_RAIL.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<MaterialBlockItem> FIELD_HEAD = ITEMS.registerItem("field_head",
            properties -> new MaterialBlockItem(ServusShuttlesBlocks.FIELD_HEAD.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<ElevatorItem> ELEVATOR = ITEMS.registerItem("elevator",
            properties -> new ElevatorItem(ServusShuttlesBlocks.ELEVATOR.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<MaterialBlockItem> INBOUND_STATION = ITEMS.registerItem("inbound_station",
            properties -> new MaterialBlockItem(ServusShuttlesBlocks.INBOUND_STATION.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<MaterialBlockItem> PACKING_STATION = ITEMS.registerItem("packing_station",
            properties -> new MaterialBlockItem(ServusShuttlesBlocks.PACKING_STATION.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<MaterialBlockItem> SHUTTLE_GARAGE = ITEMS.registerItem("shuttle_garage",
            properties -> new MaterialBlockItem(ServusShuttlesBlocks.SHUTTLE_GARAGE.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<MaterialBlockItem> OUTBOUND_STATION = ITEMS.registerItem("outbound_station",
            properties -> new MaterialBlockItem(ServusShuttlesBlocks.OUTBOUND_STATION.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<MaterialBlockItem> CRAFTING_STATION = ITEMS.registerItem("crafting_station",
            properties -> new MaterialBlockItem(ServusShuttlesBlocks.CRAFTING_STATION.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<MaterialBlockItem> CRAFTING_CPU = ITEMS.registerItem("crafting_cpu",
            properties -> new MaterialBlockItem(ServusShuttlesBlocks.CRAFTING_CPU.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<MaterialBlockItem> CPU_BUFFER = ITEMS.registerItem("crafting_cpu_buffer",
            properties -> new MaterialBlockItem(ServusShuttlesBlocks.CPU_BUFFER.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<BlockItem> SHUTTLE_WORKSHOP = ITEMS.registerSimpleBlockItem(ServusShuttlesBlocks.SHUTTLE_WORKSHOP);
    public static final DeferredItem<ShuttleItem> SHUTTLE_MK1 = ITEMS.registerItem("shuttle",
            properties -> new ShuttleItem(1, properties), () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<ShuttleItem> SHUTTLE_MK2 = ITEMS.registerItem("shuttle_mk2",
            properties -> new ShuttleItem(2, properties), () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<ShuttleItem> SHUTTLE_MK3 = ITEMS.registerItem("shuttle_mk3",
            properties -> new ShuttleItem(3, properties), () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> HANDLING_UPGRADE = ITEMS.registerSimpleItem("handling_upgrade");
    public static final DeferredItem<Item> RANGE_UPGRADE = ITEMS.registerSimpleItem("range_upgrade");
    public static final DeferredItem<Item> MUFFLER = ITEMS.registerSimpleItem("muffler");
    public static final DeferredItem<WirelessUpgradeItem> WIRELESS_UPGRADE =
            ITEMS.registerItem("wireless_upgrade", WirelessUpgradeItem::new,
                    () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<ShuttleModuleItem> HARVESTER = ITEMS.registerItem("harvester",
            properties -> new ShuttleModuleItem(new ModuleSpec(ModuleSpec.HARVESTER, true, true), properties),
            () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<ShuttleModuleItem> SPREADER = ITEMS.registerItem("spreader",
            properties -> new ShuttleModuleItem(new ModuleSpec(ModuleSpec.SPREADER, true, true), properties),
            () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<ShuttleModuleItem> LUMBER = ITEMS.registerItem("lumber",
            properties -> new ShuttleModuleItem(new ModuleSpec(ModuleSpec.LUMBER, true, true), properties),
            () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<ShuttleModuleItem> TANKER = ITEMS.registerItem("tanker",
            properties -> new ShuttleModuleItem(new ModuleSpec(ModuleSpec.TANKER, true, false), properties),
            () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<FluidStationItem> FLUID_INBOUND = ITEMS.registerItem("fluid_inbound",
            properties -> new FluidStationItem(ServusShuttlesBlocks.FLUID_INBOUND.get(), properties),
            () -> new Item.Properties().useBlockDescriptionPrefix().stacksTo(16));
    public static final DeferredItem<FluidStationItem> FLUID_OUTBOUND = ITEMS.registerItem("fluid_outbound",
            properties -> new FluidStationItem(ServusShuttlesBlocks.FLUID_OUTBOUND.get(), properties),
            () -> new Item.Properties().useBlockDescriptionPrefix().stacksTo(16));
    public static final DeferredItem<HandheldItem> HANDHELD = ITEMS.registerItem("handheld", HandheldItem::new, () -> new Item.Properties().stacksTo(1));

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(HEAVY_RACK, WAREHOUSE_CONTROLLER, WIRELESS_UPGRADE, STORAGE_TERMINAL, RAIL, ELEVATOR, INBOUND_STATION, PACKING_STATION, SHUTTLE_GARAGE, OUTBOUND_STATION, FLUID_INBOUND, FLUID_RACK, FLUID_OUTBOUND, CRAFTING_STATION, CRAFTING_CPU, CPU_BUFFER, PRODUCTION_TERMINAL, PATTERN_STORAGE, SHUTTLE_WORKSHOP, SHUTTLE_MK1, SHUTTLE_MK2, SHUTTLE_MK3, HANDLING_UPGRADE, MUFFLER, HARVESTER, SPREADER, LUMBER, TANKER, FARM_RAIL, FIELD_HEAD, RANGE_UPGRADE, HANDHELD);

    private ServusShuttlesItems() {
    }
}
