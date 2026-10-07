package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.storage.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.storage.menu.MachineDockMenu;
import io.github.dirnei.mc.servus.storage.menu.CraftingStationMenu;
import io.github.dirnei.mc.servus.storage.menu.HeavyRackMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ServusStorageIds.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CounterTerminalMenu>> COUNTER_TERMINAL =
            MENUS.register("counter_terminal", () -> IMenuTypeExtension.create(CounterTerminalMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<HeavyRackMenu>> HEAVY_RACK =
            MENUS.register("heavy_rack", () -> IMenuTypeExtension.create(HeavyRackMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<InboundStationMenu>> INBOUND_STATION =
            MENUS.register("inbound_station", () -> IMenuTypeExtension.create(InboundStationMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ZoneLabelMenu>> ZONE_LABEL =
            MENUS.register("zone_label", () -> IMenuTypeExtension.create(ZoneLabelMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ShuttleGarageMenu>> SHUTTLE_GARAGE =
            MENUS.register("shuttle_garage", () -> IMenuTypeExtension.create(ShuttleGarageMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<MachineDockMenu>> MACHINE_DOCK =
            MENUS.register("machine_dock", () -> IMenuTypeExtension.create(MachineDockMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingStationMenu>> CRAFTING_STATION =
            MENUS.register("crafting_station", () -> IMenuTypeExtension.create(CraftingStationMenu::fromNetwork));

    private ServusStorageMenus() {
    }
}
