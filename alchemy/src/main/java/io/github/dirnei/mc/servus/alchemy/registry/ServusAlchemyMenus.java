package io.github.dirnei.mc.servus.alchemy.registry;

import io.github.dirnei.mc.servus.alchemy.CoagulatorMenu;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusAlchemyMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ServusAlchemyIds.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CoagulatorMenu>> COAGULATOR =
            MENUS.register("coagulator", () -> IMenuTypeExtension.create(CoagulatorMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<GnomeHollowMenu>> GNOME_HOLLOW =
            MENUS.register("gnome_hollow", () -> IMenuTypeExtension.create(GnomeHollowMenu::fromNetwork));

    private ServusAlchemyMenus() {
    }
}
