package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client only mod bus hooks. */
@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID, value = Dist.CLIENT)
public final class ServusManufacturingClient {

    private ServusManufacturingClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ServusManufacturingMenus.BURNER_EXTRACTOR.get(), MachineScreen::burner);
        event.register(ServusManufacturingMenus.BURNER_SMELTER.get(), MachineScreen::burner);
        event.register(ServusManufacturingMenus.COAL_GENERATOR.get(), MachineScreen::generator);
        event.register(ServusManufacturingMenus.ASSEMBLER.get(), AssemblerScreen::new);
        event.register(ServusManufacturingMenus.RESEARCH_LAB.get(), ResearchLabScreen::new);
        event.register(ServusManufacturingMenus.ELECTRIC_EXTRACTOR.get(), MachineScreen::electric);
        event.register(ServusManufacturingMenus.CRUSHER.get(), MachineScreen::crusher);
        event.register(ServusManufacturingMenus.FILTER_DUCT.get(), FilterDuctScreen::new);
        event.register(ServusManufacturingMenus.CHUNK_LOADER.get(), ChunkLoaderScreen::new);
    }
}
