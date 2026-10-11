package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.foundation.client.SideMarkerRenderer;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModuleItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
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
        event.register(ServusStorageMenus.ELEVATOR.get(), ElevatorScreen::new);
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

    /** The shuttle parts are standalone models the shuttle item model composes. */
    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        List<String> tools = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ShuttleModuleItem module) {
                tools.add(module.spec().id());
            }
        }
        ShuttleItemModel.parts(tools).forEach(event::register);
    }

    /**
     * Wraps every rack block state model and the rack item model so the boards
     * show the rack's wood, and the shuttle item models so they show the
     * shuttle's parts.
     */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            ModelResourceLocation key = entry.getKey();
            if (key.id().getNamespace().equals(ServusStorageIds.MOD_ID)
                    && key.id().getPath().equals("heavy_rack")
                    && !(entry.getValue() instanceof HeavyRackModel)) {
                entry.setValue(new HeavyRackModel(entry.getValue()));
            }
            if (key.id().getNamespace().equals(ServusStorageIds.MOD_ID)
                    && ModelResourceLocation.INVENTORY_VARIANT.equals(key.variant())
                    && ShuttleItemModel.ITEMS.contains(key.id().getPath())
                    && !(entry.getValue() instanceof ShuttleItemModel)) {
                entry.setValue(new ShuttleItemModel(entry.getValue()));
            }
        }
    }
}
