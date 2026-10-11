package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.foundation.client.SideMarkerRenderer;
import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client only mod bus hooks. */
@EventBusSubscriber(modid = ServusStorageIds.MOD_ID, value = Dist.CLIENT)
public final class ServusStorageClient {

    private ServusStorageClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ServusStorageMenus.STORAGE_TERMINAL.get(), StorageTerminalScreen::new);
        event.register(ServusStorageMenus.HEAVY_RACK.get(), HeavyRackScreen::new);
        event.register(ServusStorageMenus.INBOUND_STATION.get(), InboundStationScreen::new);
        event.register(ServusStorageMenus.ZONE_LABEL.get(), ZoneLabelScreen::new);
        event.register(ServusStorageMenus.SHUTTLE_GARAGE.get(), ShuttleGarageScreen::new);
        event.register(ServusStorageMenus.SHUTTLE_WORKSHOP.get(), ShuttleWorkshopScreen::new);
        event.register(ServusStorageMenus.FIELD_HEAD.get(), FieldHeadScreen::new);
        event.register(ServusStorageMenus.PACKING_STATION.get(), PackingStationScreen::new);
        event.register(ServusStorageMenus.OUTBOUND_STATION.get(), OutboundStationScreen::new);
        event.register(ServusStorageMenus.CRAFTING_STATION.get(), CraftingStationScreen::new);
        event.register(ServusStorageMenus.PRODUCTION_TERMINAL.get(), ProductionTerminalScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.CRATE.get(), CrateRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.INBOUND_STATION.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.OUTBOUND_STATION.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.CRAFTING_STATION.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.HEAVY_RACK.get(), HeavyRackRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.SHUTTLE_GARAGE.get(), ShuttleGarageRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.ELEVATOR.get(), ElevatorRenderer::new);
    }

    /** Wraps every rack block state model and the rack item model so the boards show the rack's wood. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            ModelResourceLocation key = entry.getKey();
            if (key.id().getNamespace().equals(ServusStorageIds.MOD_ID)
                    && key.id().getPath().equals("heavy_rack")
                    && !(entry.getValue() instanceof HeavyRackModel)) {
                entry.setValue(new HeavyRackModel(entry.getValue()));
            }
        }
    }
}
