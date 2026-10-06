package io.github.dirnei.mc.servus.lib.registry;

import io.github.dirnei.mc.servus.lib.ServusLibIds;
import io.github.dirnei.mc.servus.lib.item.GogglesItem;
import io.github.dirnei.mc.servus.lib.item.ConfigurationCarrotItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLibItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusLibIds.MOD_ID);

    public static final DeferredItem<ConfigurationCarrotItem> CONFIGURATION_CARROT =
            ITEMS.registerItem("configuration_carrot", ConfigurationCarrotItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<GogglesItem> GOGGLES =
            ITEMS.registerItem("engineer_goggles", GogglesItem::new, new Item.Properties());

    private ServusLibItems() {
    }
}
