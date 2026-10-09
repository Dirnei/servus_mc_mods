package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.menu.AssemblerMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.BurnerExtractorMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.BurnerPumpMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.BurnerSmelterMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.ChunkLoaderMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.CrusherMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.ElectricExtractorMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.ElectricPumpMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.ResearchLabMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.SinkMenu;
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


    public static final DeferredHolder<MenuType<?>, MenuType<AssemblerMenu>> ASSEMBLER =
            MENUS.register("assembler", () -> IMenuTypeExtension.create(AssemblerMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricExtractorMenu>> ELECTRIC_EXTRACTOR =
            MENUS.register("electric_extractor", () -> IMenuTypeExtension.create(ElectricExtractorMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<CrusherMenu>> CRUSHER =
            MENUS.register("crusher", () -> IMenuTypeExtension.create(CrusherMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ChunkLoaderMenu>> CHUNK_LOADER =
            MENUS.register("chunk_loader", () -> IMenuTypeExtension.create(ChunkLoaderMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ResearchLabMenu>> RESEARCH_LAB =
            MENUS.register("research_lab", () -> IMenuTypeExtension.create(ResearchLabMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<BurnerPumpMenu>> BURNER_PUMP =
            MENUS.register("burner_pump", () -> IMenuTypeExtension.create(BurnerPumpMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricPumpMenu>> ELECTRIC_PUMP =
            MENUS.register("electric_pump", () -> IMenuTypeExtension.create(ElectricPumpMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<SinkMenu>> SINK =
            MENUS.register("sink", () -> IMenuTypeExtension.create(SinkMenu::fromNetwork));

    private ServusManufacturingMenus() {
    }
}
