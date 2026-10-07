package io.github.dirnei.mc.servus.logistics.registry;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.Line;
import io.github.dirnei.mc.servus.logistics.duct.LineItem;
import io.github.dirnei.mc.servus.logistics.facade.FacadeItem;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLogisticsItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusLogisticsIds.MOD_ID);

    public static final DeferredItem<LineItem> ITEM_PIPE = ITEMS.registerItem("item_pipe",
            properties -> new LineItem(ServusLogisticsBlocks.UTILITY_DUCT.get(), Line.ITEM, properties));
    public static final DeferredItem<LineItem> POWER_CABLE = ITEMS.registerItem("power_cable",
            properties -> new LineItem(ServusLogisticsBlocks.UTILITY_DUCT.get(), Line.ENERGY, properties));

    public static final DeferredItem<BlockItem> FLUID_TANK = ITEMS.registerSimpleBlockItem(ServusLogisticsBlocks.FLUID_TANK);

    public static final DeferredItem<FacadeItem> FACADE = ITEMS.registerItem("facade", FacadeItem::new, new Item.Properties());

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(ITEM_PIPE, POWER_CABLE, FLUID_TANK);

    private ServusLogisticsItems() {
    }

    public static Item lineItem(Line line) {
        return line == Line.ITEM ? ITEM_PIPE.get() : POWER_CABLE.get();
    }
}
