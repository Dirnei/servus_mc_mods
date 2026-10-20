package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.foundation.wood.WoodBlockItem;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.controller.WirelessUpgradeItem;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryItem;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackItem;
import io.github.dirnei.mc.servus.shuttles.rail.ElevatorItem;
import io.github.dirnei.mc.servus.shuttles.rail.RailItem;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ModuleSpec;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModuleItem;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneLabelItem;
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
    public static final DeferredItem<WoodBlockItem> WAREHOUSE_CONTROLLER = ITEMS.registerItem("warehouse_controller",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WoodBlockItem> STORAGE_TERMINAL = ITEMS.registerItem("storage_terminal",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.STORAGE_TERMINAL.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WoodBlockItem> PRODUCTION_TERMINAL = ITEMS.registerItem("production_terminal",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.PRODUCTION_TERMINAL.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WoodBlockItem> PATTERN_STORAGE = ITEMS.registerItem("pattern_storage",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.PATTERN_STORAGE.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<RailItem> RAIL = ITEMS.registerItem("rail",
            properties -> new RailItem(ServusShuttlesBlocks.RAIL.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<RailItem> FARM_RAIL = ITEMS.registerItem("farm_rail",
            properties -> new RailItem(ServusShuttlesBlocks.FARM_RAIL.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WoodBlockItem> FIELD_HEAD = ITEMS.registerItem("field_head",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.FIELD_HEAD.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<ElevatorItem> ELEVATOR = ITEMS.registerItem("elevator",
            properties -> new ElevatorItem(ServusShuttlesBlocks.ELEVATOR.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WoodBlockItem> INBOUND_STATION = ITEMS.registerItem("inbound_station",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.INBOUND_STATION.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WoodBlockItem> PACKING_STATION = ITEMS.registerItem("packing_station",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.PACKING_STATION.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WoodBlockItem> SHUTTLE_GARAGE = ITEMS.registerItem("shuttle_garage",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.SHUTTLE_GARAGE.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WoodBlockItem> OUTBOUND_STATION = ITEMS.registerItem("outbound_station",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.OUTBOUND_STATION.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WoodBlockItem> CRAFTING_STATION = ITEMS.registerItem("crafting_station",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.CRAFTING_STATION.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WoodBlockItem> CRAFTING_CPU = ITEMS.registerItem("crafting_cpu",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.CRAFTING_CPU.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<WoodBlockItem> CPU_BUFFER = ITEMS.registerItem("crafting_cpu_buffer",
            properties -> new WoodBlockItem(ServusShuttlesBlocks.CPU_BUFFER.get(), properties),
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
    public static final DeferredItem<WirelessUpgradeItem> WIRELESS_UPGRADE =
            ITEMS.registerItem("wireless_upgrade", WirelessUpgradeItem::new,
                    () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<ShuttleModuleItem> HARVESTER = ITEMS.registerItem("harvester",
            properties -> new ShuttleModuleItem(new ModuleSpec(ModuleSpec.HARVESTER, true, true), properties),
            () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<ShuttleModuleItem> SPREADER = ITEMS.registerItem("spreader",
            properties -> new ShuttleModuleItem(new ModuleSpec(ModuleSpec.SPREADER, true, true), properties),
            () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<FluidGantryItem> FLUID_GANTRY = ITEMS.registerItem("fluid_gantry",
            properties -> new FluidGantryItem(ServusShuttlesBlocks.FLUID_GANTRY.get(), properties),
            () -> new Item.Properties().useBlockDescriptionPrefix().stacksTo(16));
    public static final DeferredItem<ZoneLabelItem> ZONE_LABEL = ITEMS.registerItem("zone_label", ZoneLabelItem::new, () -> new Item.Properties().stacksTo(1));

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(HEAVY_RACK, WAREHOUSE_CONTROLLER, WIRELESS_UPGRADE, STORAGE_TERMINAL, RAIL, ELEVATOR, INBOUND_STATION, PACKING_STATION, SHUTTLE_GARAGE, OUTBOUND_STATION, FLUID_GANTRY, CRAFTING_STATION, CRAFTING_CPU, CPU_BUFFER, PRODUCTION_TERMINAL, PATTERN_STORAGE, SHUTTLE_WORKSHOP, SHUTTLE_MK1, SHUTTLE_MK2, SHUTTLE_MK3, HANDLING_UPGRADE, HARVESTER, SPREADER, FARM_RAIL, FIELD_HEAD, RANGE_UPGRADE, ZONE_LABEL);

    private ServusShuttlesItems() {
    }
}
