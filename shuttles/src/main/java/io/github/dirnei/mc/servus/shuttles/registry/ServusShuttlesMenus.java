package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.shuttles.menu.PackingStationMenu;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.shuttles.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.menu.CraftingStationMenu;
import io.github.dirnei.mc.servus.shuttles.menu.HeavyRackMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ElevatorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import io.github.dirnei.mc.servus.shuttles.menu.ProductionTerminalMenu;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusShuttlesMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ServusShuttlesIds.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<StorageTerminalMenu>> STORAGE_TERMINAL =
            MENUS.register("storage_terminal", () -> IMenuTypeExtension.create(StorageTerminalMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ProductionTerminalMenu>> PRODUCTION_TERMINAL =
            MENUS.register("production_terminal", () -> IMenuTypeExtension.create(ProductionTerminalMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<HeavyRackMenu>> HEAVY_RACK =
            MENUS.register("heavy_rack", () -> IMenuTypeExtension.create(HeavyRackMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<InboundStationMenu>> INBOUND_STATION =
            MENUS.register("inbound_station", () -> IMenuTypeExtension.create(InboundStationMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<PackingStationMenu>> PACKING_STATION =
            MENUS.register("packing_station", () -> IMenuTypeExtension.create(PackingStationMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<WarehouseControllerMenu>> WAREHOUSE_CONTROLLER =
            MENUS.register("warehouse_controller", () -> IMenuTypeExtension.create(WarehouseControllerMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ElevatorMenu>> ELEVATOR =
            MENUS.register("elevator", () -> IMenuTypeExtension.create(ElevatorMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ZoneLabelMenu>> ZONE_LABEL =
            MENUS.register("zone_label", () -> IMenuTypeExtension.create(ZoneLabelMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ShuttleGarageMenu>> SHUTTLE_GARAGE =
            MENUS.register("shuttle_garage", () -> IMenuTypeExtension.create(ShuttleGarageMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<OutboundStationMenu>> OUTBOUND_STATION =
            MENUS.register("outbound_station", () -> IMenuTypeExtension.create(OutboundStationMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingStationMenu>> CRAFTING_STATION =
            MENUS.register("crafting_station", () -> IMenuTypeExtension.create(CraftingStationMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ShuttleWorkshopMenu>> SHUTTLE_WORKSHOP =
            MENUS.register("shuttle_workshop", () -> IMenuTypeExtension.create(ShuttleWorkshopMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<FieldHeadMenu>> FIELD_HEAD =
            MENUS.register("field_head", () -> IMenuTypeExtension.create(FieldHeadMenu::fromNetwork));

    private ServusShuttlesMenus() {
    }
}
