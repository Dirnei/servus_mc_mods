package io.github.dirnei.mc.servus.lib.registry;

import io.github.dirnei.mc.servus.lib.ServusLibIds;
import io.github.dirnei.mc.servus.lib.item.WrenchItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLibItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusLibIds.MOD_ID);

    public static final DeferredItem<WrenchItem> WRENCH =
            ITEMS.registerItem("wrench", WrenchItem::new, new Item.Properties().stacksTo(1));

    private ServusLibItems() {
    }
}
