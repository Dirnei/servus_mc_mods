package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client only mod bus hooks. */
@EventBusSubscriber(modid = ServusStorageIds.MOD_ID, value = Dist.CLIENT)
public final class ServusStorageClient {

    private ServusStorageClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ServusStorageMenus.COUNTER_TERMINAL.get(), CounterTerminalScreen::new);
        event.register(ServusStorageMenus.HEAVY_RACK.get(), HeavyRackScreen::new);
        event.register(ServusStorageMenus.DISPATCH_STATION.get(), DispatchStationScreen::new);
        event.register(ServusStorageMenus.ZONE_LABEL.get(), ZoneLabelScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.CRATE.get(), CrateRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.HEAVY_RACK.get(), HeavyRackRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.WAREHOUSE_CONTROLLER.get(), WarehouseControllerRenderer::new);
    }
}
