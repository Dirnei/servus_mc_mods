package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.menu.BurnerExtractorMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.BurnerSmelterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ServusManufacturingIds.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<BurnerExtractorMenu>> BURNER_EXTRACTOR =
            MENUS.register("burner_extractor", () -> IMenuTypeExtension.create(BurnerExtractorMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<BurnerSmelterMenu>> BURNER_SMELTER =
            MENUS.register("burner_smelter", () -> IMenuTypeExtension.create(BurnerSmelterMenu::fromNetwork));

    private ServusManufacturingMenus() {
    }
}
