package io.github.dirnei.mc.servus.foundation.registry;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.filter.FilterItem;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.DataPotatoItem;
import io.github.dirnei.mc.servus.foundation.item.GogglesItem;
import io.github.dirnei.mc.servus.foundation.upgrade.CapacityUpgradeItem;
import io.github.dirnei.mc.servus.foundation.upgrade.HeatingElementItem;
import io.github.dirnei.mc.servus.foundation.upgrade.UpgradeItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusFoundationItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusFoundationIds.MOD_ID);

    public static final DeferredItem<ConfigurationCarrotItem> CONFIGURATION_CARROT =
            ITEMS.registerItem("configuration_carrot", ConfigurationCarrotItem::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<GogglesItem> GOGGLES =
            ITEMS.registerItem("engineer_goggles", GogglesItem::new, () -> new Item.Properties());
    public static final DeferredItem<DataPotatoItem> DATA_POTATO =
            ITEMS.registerItem("data_potato", DataPotatoItem::new, () -> new Item.Properties());

    public static final DeferredItem<UpgradeItem> SPEED_UPGRADE =
            ITEMS.registerItem("speed_upgrade", properties -> new UpgradeItem(2, properties), () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<CapacityUpgradeItem> CAPACITY_UPGRADE =
            ITEMS.registerItem("capacity_upgrade", CapacityUpgradeItem::new, () -> new Item.Properties().stacksTo(16));

    public static final DeferredItem<HeatingElementItem> HEATING_ELEMENT =
            ITEMS.registerItem("heating_element", HeatingElementItem::new, () -> new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> CHARCOAL_POWDER = ITEMS.registerSimpleItem("charcoal_powder");
    public static final DeferredItem<Item> ACTIVATED_CHARCOAL = ITEMS.registerSimpleItem("activated_charcoal");
    public static final DeferredItem<FilterItem> CARBON_FILTER =
            ITEMS.registerItem("carbon_filter", FilterItem::new, () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> SPENT_FILTER = ITEMS.registerSimpleItem("spent_filter");

    private ServusFoundationItems() {
    }
}
