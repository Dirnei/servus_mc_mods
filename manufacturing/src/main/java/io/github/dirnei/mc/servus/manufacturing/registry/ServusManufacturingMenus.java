package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ServusManufacturingIds.MOD_ID);


    public static final DeferredHolder<MenuType<?>, MenuType<CalcinerMenu>> CALCINER =
            MENUS.register("calciner", () -> IMenuTypeExtension.create(CalcinerMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<CoagulatorMenu>> COAGULATOR =
            MENUS.register("coagulator", () -> IMenuTypeExtension.create(CoagulatorMenu::fromNetwork));

    private ServusManufacturingMenus() {
    }
}
