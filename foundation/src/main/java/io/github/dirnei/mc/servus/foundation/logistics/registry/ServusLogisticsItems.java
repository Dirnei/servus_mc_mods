package io.github.dirnei.mc.servus.foundation.logistics.registry;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DataCableItem;
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
    public static final DeferredItem<DataCableItem> DATA_CABLE = ITEMS.registerItem("data_cable",
            properties -> new DataCableItem(ServusLogisticsBlocks.UTILITY_DUCT.get(), 0xF4F4F4, "", properties));
    public static final DeferredItem<DataCableItem> DUPLEX_DATA_CABLE = ITEMS.registerItem("duplex_data_cable",
            properties -> new DataCableItem(ServusLogisticsBlocks.UTILITY_DUCT.get(), 0x8C8E92, "duplex", properties));
    public static final DeferredItem<DataCableItem> FAST_DATA_CABLE = ITEMS.registerItem("fast_data_cable",
            properties -> new DataCableItem(ServusLogisticsBlocks.UTILITY_DUCT.get(), 0x3C3E44, "fast", properties));

    public static final DeferredItem<FluidTankItem> FLUID_TANK = ITEMS.registerItem("fluid_tank",
            properties -> new FluidTankItem(ServusLogisticsBlocks.FLUID_TANK.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);

    public static final DeferredItem<FacadeItem> FACADE = ITEMS.registerItem("facade",
            properties -> new FacadeItem(properties, false));
    public static final DeferredItem<FacadeItem> HOLLOW_FACADE = ITEMS.registerItem("hollow_facade",
            properties -> new FacadeItem(properties, true));

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(ITEM_PIPE, POWER_CABLE, FLUID_PIPE, DATA_CABLE,
            DUPLEX_DATA_CABLE, FAST_DATA_CABLE, FLUID_TANK);

    private ServusLogisticsItems() {
    }

    /** The base item of a line; the data line's tier item comes from the duct, see {@code UtilityDuctBlockEntity#lineItem}. */
    public static Item lineItem(Line line) {
        return switch (line) {
            case ITEM -> ITEM_PIPE.get();
            case ENERGY -> POWER_CABLE.get();
            case FLUID -> FLUID_PIPE.get();
            case DATA -> DATA_CABLE.get();
        };
    }
}
