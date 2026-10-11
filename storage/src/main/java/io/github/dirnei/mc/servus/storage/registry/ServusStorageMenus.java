package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.storage.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.storage.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.storage.menu.CraftingStationMenu;
import io.github.dirnei.mc.servus.storage.menu.HeavyRackMenu;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ServusStorageIds.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<StorageTerminalMenu>> STORAGE_TERMINAL =
            MENUS.register("storage_terminal", () -> IMenuTypeExtension.create(StorageTerminalMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ProductionTerminalMenu>> PRODUCTION_TERMINAL =
            MENUS.register("production_terminal", () -> IMenuTypeExtension.create(ProductionTerminalMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<HeavyRackMenu>> HEAVY_RACK =
            MENUS.register("heavy_rack", () -> IMenuTypeExtension.create(HeavyRackMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<InboundStationMenu>> INBOUND_STATION =
            MENUS.register("inbound_station", () -> IMenuTypeExtension.create(InboundStationMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.dirnei.mc.servus.storage.menu.PackingStationMenu>> PACKING_STATION =
            MENUS.register("packing_station", () -> IMenuTypeExtension.create(io.github.dirnei.mc.servus.storage.menu.PackingStationMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ZoneLabelMenu>> ZONE_LABEL =
            MENUS.register("zone_label", () -> IMenuTypeExtension.create(ZoneLabelMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ShuttleGarageMenu>> SHUTTLE_GARAGE =
            MENUS.register("shuttle_garage", () -> IMenuTypeExtension.create(ShuttleGarageMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<OutboundStationMenu>> OUTBOUND_STATION =
            MENUS.register("outbound_station", () -> IMenuTypeExtension.create(OutboundStationMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingStationMenu>> CRAFTING_STATION =
            MENUS.register("crafting_station", () -> IMenuTypeExtension.create(CraftingStationMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ShuttleWorkshopMenu>> SHUTTLE_WORKSHOP =
            MENUS.register("shuttle_workshop", () -> new MenuType<>(ShuttleWorkshopMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ServusStorageMenus() {
    }
}
