package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.foundation.placement.AssistedBlockItem;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackItem;
import io.github.dirnei.mc.servus.storage.rail.ElevatorAssist;
import io.github.dirnei.mc.servus.storage.rail.RailAssist;
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
    public static final DeferredItem<BlockItem> WAREHOUSE_CONTROLLER = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.WAREHOUSE_CONTROLLER);
    public static final DeferredItem<BlockItem> COUNTER_TERMINAL = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.COUNTER_TERMINAL);
    public static final DeferredItem<AssistedBlockItem> RAIL = ITEMS.registerItem("rail",
            properties -> new AssistedBlockItem(ServusStorageBlocks.RAIL.get(), RailAssist.INSTANCE, properties));
    public static final DeferredItem<AssistedBlockItem> ELEVATOR = ITEMS.registerItem("elevator",
            properties -> new AssistedBlockItem(ServusStorageBlocks.ELEVATOR.get(), ElevatorAssist.INSTANCE, properties));
    public static final DeferredItem<BlockItem> INBOUND_STATION = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.INBOUND_STATION);
    public static final DeferredItem<BlockItem> ACCESS_POINT = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.ACCESS_POINT);
    public static final DeferredItem<BlockItem> SHUTTLE_GARAGE = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.SHUTTLE_GARAGE);
    public static final DeferredItem<BlockItem> OUTBOUND_STATION = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.OUTBOUND_STATION);
    public static final DeferredItem<BlockItem> CRAFTING_STATION = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.CRAFTING_STATION);
    public static final DeferredItem<BlockItem> CRAFTING_CPU = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.CRAFTING_CPU);
    public static final DeferredItem<Item> SHUTTLE = ITEMS.registerSimpleItem("shuttle", new Item.Properties().stacksTo(16));
    public static final DeferredItem<ZoneLabelItem> ZONE_LABEL = ITEMS.registerItem("zone_label", ZoneLabelItem::new, new Item.Properties().stacksTo(1));

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(CRATE, DOUBLE_CRATE, QUAD_CRATE, HEAVY_RACK, WAREHOUSE_CONTROLLER, COUNTER_TERMINAL, RAIL, ELEVATOR, INBOUND_STATION, ACCESS_POINT, SHUTTLE_GARAGE, OUTBOUND_STATION, CRAFTING_STATION, CRAFTING_CPU, SHUTTLE, ZONE_LABEL);

    private ServusStorageItems() {
    }
}
