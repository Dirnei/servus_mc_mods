package io.github.dirnei.mc.servus.foundation.logistics.registry;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.menu.UtilityDuctMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLogisticsMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ServusFoundationIds.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<UtilityDuctMenu>> UTILITY_DUCT =
            MENUS.register("utility_duct", () -> IMenuTypeExtension.create(UtilityDuctMenu::fromNetwork));

    private ServusLogisticsMenus() {
    }
}
