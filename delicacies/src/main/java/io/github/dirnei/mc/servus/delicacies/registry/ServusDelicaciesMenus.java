package io.github.dirnei.mc.servus.delicacies.registry;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The menu types of Servus: Delicacies. */
public final class ServusDelicaciesMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ServusDelicaciesIds.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AquariumMenu>> AQUARIUM =
            MENUS.register("aquarium", () -> IMenuTypeExtension.create(AquariumMenu::fromNetwork));

    private ServusDelicaciesMenus() {
    }
}
