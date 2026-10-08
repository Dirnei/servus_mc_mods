package io.github.dirnei.mc.servus.foundation.logistics.registry;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.LineItem;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeItem;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankItem;
import java.util.List;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLogisticsItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusFoundationIds.MOD_ID);

    public static final DeferredItem<LineItem> ITEM_PIPE = ITEMS.registerItem("item_pipe",
            properties -> new LineItem(ServusLogisticsBlocks.UTILITY_DUCT.get(), Line.ITEM, properties));
    public static final DeferredItem<LineItem> POWER_CABLE = ITEMS.registerItem("power_cable",
            properties -> new LineItem(ServusLogisticsBlocks.UTILITY_DUCT.get(), Line.ENERGY, properties));
    public static final DeferredItem<LineItem> FLUID_PIPE = ITEMS.registerItem("fluid_pipe",
            properties -> new LineItem(ServusLogisticsBlocks.UTILITY_DUCT.get(), Line.FLUID, properties));

    public static final DeferredItem<FluidTankItem> FLUID_TANK = ITEMS.registerItem("fluid_tank",
            properties -> new FluidTankItem(ServusLogisticsBlocks.FLUID_TANK.get(), properties));

    public static final DeferredItem<FacadeItem> FACADE = ITEMS.registerItem("facade", FacadeItem::new, new Item.Properties());

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(ITEM_PIPE, POWER_CABLE, FLUID_PIPE, FLUID_TANK);

    private ServusLogisticsItems() {
    }

    public static Item lineItem(Line line) {
        return switch (line) {
            case ITEM -> ITEM_PIPE.get();
            case ENERGY -> POWER_CABLE.get();
            case FLUID -> FLUID_PIPE.get();
        };
    }
}
