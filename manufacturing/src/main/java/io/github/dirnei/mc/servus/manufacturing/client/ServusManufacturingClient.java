package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirnei.mc.servus.manufacturing.client.sight.HollowRevealRenderer;
import io.github.dirnei.mc.servus.manufacturing.client.sight.RevealRenderers;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

/** Client only mod bus hooks. */
@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID, value = Dist.CLIENT)
public final class ServusManufacturingClient {

    private ServusManufacturingClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ServusManufacturingMenus.COAGULATOR.get(), CoagulatorScreen::new);
        event.register(ServusManufacturingMenus.GNOME_HOLLOW.get(), GnomeHollowScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.CALCINER.get(), CalcinerRenderer::new);
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.PURIFIER.get(), PurifierRenderer::new);
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.COAGULATOR.get(), CoagulatorRenderer::new);
        event.registerBlockEntityRenderer(ServusManufacturingBlockEntities.ATHANOR.get(), AthanorRenderer::new);
    }

    /** Dev only icon export for the docs: its renderer exists only when the iconExport run asks for it. */
    @SubscribeEvent
    public static void onRegisterPictureInPicture(RegisterPictureInPictureRenderersEvent event) {
        IconExporter.registerRendererIfRequested(event);
    }

    /** The dev only icon export, when the iconExport run asks for it, and the Elemental Sight renderers. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        IconExporter.registerIfRequested();
        RevealRenderers.add(new HollowRevealRenderer());
    }
}
