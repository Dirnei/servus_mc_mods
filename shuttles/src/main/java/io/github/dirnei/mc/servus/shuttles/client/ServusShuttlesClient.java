package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.foundation.client.SideMarkerRenderer;
import java.util.Map;
import io.github.dirnei.mc.servus.foundation.material.client.MaterialModel;
import io.github.dirnei.mc.servus.foundation.material.client.MaterialSkin;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

/** Client only mod bus hooks. */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID, value = Dist.CLIENT)
public final class ServusShuttlesClient {

    private ServusShuttlesClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ServusShuttlesMenus.STORAGE_TERMINAL.get(), StorageTerminalScreen::new);
        event.register(ServusShuttlesMenus.HEAVY_RACK.get(), HeavyRackScreen::new);
        event.register(ServusShuttlesMenus.INBOUND_STATION.get(), InboundStationScreen::new);
        event.register(ServusShuttlesMenus.ZONE_LABEL.get(), ZoneLabelScreen::new);
        event.register(ServusShuttlesMenus.SHUTTLE_GARAGE.get(), ShuttleGarageScreen::new);
        event.register(ServusShuttlesMenus.SHUTTLE_WORKSHOP.get(), ShuttleWorkshopScreen::new);
        event.register(ServusShuttlesMenus.FIELD_HEAD.get(), FieldHeadScreen::new);
        event.register(ServusShuttlesMenus.PACKING_STATION.get(), PackingStationScreen::new);
        event.register(ServusShuttlesMenus.ELEVATOR.get(), ElevatorScreen::new);
        event.register(ServusShuttlesMenus.FLUID_GANTRY.get(), FluidGantryScreen::new);
        event.register(ServusShuttlesMenus.WAREHOUSE_CONTROLLER.get(), WarehouseControllerScreen::new);
        event.register(ServusShuttlesMenus.OUTBOUND_STATION.get(), OutboundStationScreen::new);
        event.register(ServusShuttlesMenus.CRAFTING_STATION.get(), CraftingStationScreen::new);
        event.register(ServusShuttlesMenus.PRODUCTION_TERMINAL.get(), ProductionTerminalScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ServusShuttlesBlockEntities.FIELD_HEAD.get(), FieldHeadRenderer::new);
        event.registerBlockEntityRenderer(ServusShuttlesBlockEntities.INBOUND_STATION.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusShuttlesBlockEntities.OUTBOUND_STATION.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusShuttlesBlockEntities.CRAFTING_STATION.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(ServusShuttlesBlockEntities.HEAVY_RACK.get(), HeavyRackRenderer::new);
        event.registerBlockEntityRenderer(ServusShuttlesBlockEntities.SHUTTLE_GARAGE.get(), ShuttleGarageRenderer::new);
        event.registerBlockEntityRenderer(ServusShuttlesBlockEntities.ELEVATOR.get(), ElevatorRenderer::new);
        event.registerBlockEntityRenderer(ServusShuttlesBlockEntities.FLUID_GANTRY.get(), FluidGantryRenderer::new);
    }

    /** The Shuttle Workshop draws its shuttle large as a picture in picture. */
    @SubscribeEvent
    public static void onRegisterPictureInPicture(RegisterPictureInPictureRenderersEvent event) {
        event.register(GuiLargeItemRenderState.class, GuiLargeItemRenderer::new);
    }

    /** The rack shows the wood it remembers, on placed blocks and on items. */
    public static final MaterialSkin RACK_SKIN = new MaterialSkin(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "rack_wood"),
            Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "block/heavy_rack_board"), HeavyRackBlockEntity.WOOD,
            HeavyRackBlockEntity.DEFAULT_WOOD, HeavyRackBlockEntity::woodOf, false);

    /** The rack item and the shuttle items have their own item model types. */
    @SubscribeEvent
    public static void onRegisterItemModels(RegisterItemModelsEvent event) {
        event.register(RACK_SKIN.itemModelType(), RACK_SKIN.itemCodec());
        event.register(ShuttleItemModel.TYPE, ShuttleItemModel.Unbaked.MAP_CODEC);
    }

    /** The Lumber shuttle's hold, crane and harvester head parts, posed by the garage renderer while it is unfolded. */
    @SubscribeEvent
    public static void onRegisterStandalone(ModelEvent.RegisterStandalone event) {
        HarvesterLook.register(event);
    }

    /** Wraps every rack block state model so the boards show the rack's wood. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        for (Map.Entry<BlockState, BlockStateModel> entry : models.entrySet()) {
            if (entry.getKey().is(ServusShuttlesBlocks.HEAVY_RACK.get()) && !(entry.getValue() instanceof MaterialModel)) {
                entry.setValue(new MaterialModel(entry.getValue(), RACK_SKIN));
            }
        }
    }
}
