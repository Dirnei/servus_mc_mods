package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.foundation.client.SideMarkerRenderer;
import java.util.Map;
import io.github.dirnei.mc.servus.foundation.wood.client.WoodModel;
import io.github.dirnei.mc.servus.foundation.wood.client.WoodSkin;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
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
        event.register(ServusStorageMenus.WAREHOUSE_CONTROLLER.get(), WarehouseControllerScreen::new);
        event.register(ServusStorageMenus.OUTBOUND_STATION.get(), OutboundStationScreen::new);
        event.register(ServusStorageMenus.CRAFTING_STATION.get(), CraftingStationScreen::new);
        event.register(ServusStorageMenus.PRODUCTION_TERMINAL.get(), ProductionTerminalScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.CRATE.get(), CrateRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.FIELD_HEAD.get(), FieldHeadRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.INBOUND_STATION.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.OUTBOUND_STATION.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.CRAFTING_STATION.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.HEAVY_RACK.get(), HeavyRackRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.SHUTTLE_GARAGE.get(), ShuttleGarageRenderer::new);
        event.registerBlockEntityRenderer(ServusStorageBlockEntities.ELEVATOR.get(), ElevatorRenderer::new);
    }

    /** The rack shows the wood it remembers, on placed blocks and on items. */
    public static final WoodSkin RACK_SKIN = new WoodSkin(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "rack_wood"),
            Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "block/heavy_rack_board"), HeavyRackBlockEntity.WOOD,
            HeavyRackBlockEntity.DEFAULT_WOOD, HeavyRackBlockEntity::woodOf, false);

    /** The rack item and the shuttle items have their own item model types. */
    @SubscribeEvent
    public static void onRegisterItemModels(RegisterItemModelsEvent event) {
        event.register(RACK_SKIN.itemModelType(), RACK_SKIN.itemCodec());
        event.register(ShuttleItemModel.TYPE, ShuttleItemModel.Unbaked.MAP_CODEC);
    }

    /** Wraps every rack block state model so the boards show the rack's wood. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        for (Map.Entry<BlockState, BlockStateModel> entry : models.entrySet()) {
            if (entry.getKey().is(ServusStorageBlocks.HEAVY_RACK.get()) && !(entry.getValue() instanceof WoodModel)) {
                entry.setValue(new WoodModel(entry.getValue(), RACK_SKIN));
            }
        }
    }
}
