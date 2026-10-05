package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ServusStorageIds.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CounterTerminalMenu>> COUNTER_TERMINAL =
            MENUS.register("counter_terminal", () -> IMenuTypeExtension.create(CounterTerminalMenu::fromNetwork));

    private ServusStorageMenus() {
    }
}
