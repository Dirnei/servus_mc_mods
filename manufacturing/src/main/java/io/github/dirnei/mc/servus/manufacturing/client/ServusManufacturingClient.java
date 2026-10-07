package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.lib.client.SideMarkerRenderer;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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
        event.register(ServusManufacturingMenus.CHUNK_LOADER.get(), ChunkLoaderScreen::new);
    }

    /** Every machine draws its side modes on the block. */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.BURNER_EXTRACTOR.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.BURNER_SMELTER.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.COAL_GENERATOR.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.ASSEMBLER.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.ELECTRIC_EXTRACTOR.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.CRUSHER.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.CHUNK_LOADER.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.RESEARCH_LAB.get(), SideMarkerRenderer::new);
    }
}
