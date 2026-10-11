package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.pattern.OpenRecipeEditorPayload;
import io.github.dirnei.mc.servus.storage.client.ClientProduction;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.storage.production.CancelProductionOrderPayload;
import io.github.dirnei.mc.servus.storage.production.EditPatternPayload;
import io.github.dirnei.mc.servus.storage.production.ProductionPayload;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.client.ClientStock;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.storage.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.storage.outbound.KeepSlotPayload;
import io.github.dirnei.mc.servus.storage.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.storage.zone.SetLabelNamePayload;
import io.github.dirnei.mc.servus.storage.zone.SetLabelTagsPayload;
import io.github.dirnei.mc.servus.storage.client.ClientStationState;
import io.github.dirnei.mc.servus.storage.station.StationStatePayload;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.storage.field.ChooseFertilizerPayload;
import io.github.dirnei.mc.servus.storage.field.ChooseFieldCropPayload;
import io.github.dirnei.mc.servus.storage.field.FieldFertilizerStockPayload;
import io.github.dirnei.mc.servus.storage.field.FieldCropStockPayload;
import io.github.dirnei.mc.servus.storage.shuttle.module.EngraveShuttlePayload;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleBuildStepPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirnei.mc.servus.storage.client.ClientShuttles;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleSyncPayload;
import io.github.dirnei.mc.servus.storage.shuttle.LiftSyncPayload;
import io.github.dirnei.mc.servus.storage.client.ClientLifts;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers the terminal payloads. */
@EventBusSubscriber(modid = ServusStorageIds.MOD_ID)
public final class ServusStoragePayloads {

    private ServusStoragePayloads() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(StockSyncPayload.TYPE, StockSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientStock.update(payload));
        registrar.playToClient(ProductionPayload.TYPE, ProductionPayload.STREAM_CODEC,
                (payload, context) -> ClientProduction.update(payload));
        registrar.playToServer(CancelProductionOrderPayload.TYPE, CancelProductionOrderPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof ProductionTerminalMenu menu) {
                        menu.cancel(payload.cpu(), payload.id());
                    }
                });
        registrar.playToServer(OpenRecipeEditorPayload.TYPE, OpenRecipeEditorPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof ProductionTerminalMenu menu) {
                        menu.openEditorWith(payload.recipe());
                    }
                });
        registrar.playToServer(EditPatternPayload.TYPE, EditPatternPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof ProductionTerminalMenu menu) {
                        menu.openExisting(payload.storage(), payload.slot());
                    }
                });
        registrar.playToClient(ShuttleSyncPayload.TYPE, ShuttleSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientShuttles.update(payload));
        registrar.playToClient(LiftSyncPayload.TYPE, LiftSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientLifts.update(payload));
        registrar.playToClient(StationStatePayload.TYPE, StationStatePayload.STREAM_CODEC,
                (payload, context) -> ClientStationState.update(payload));
        registrar.playToClient(io.github.dirnei.mc.servus.storage.packing.PackingStatusPayload.TYPE,
                io.github.dirnei.mc.servus.storage.packing.PackingStatusPayload.STREAM_CODEC,
                (payload, context) -> io.github.dirnei.mc.servus.storage.client.ClientPackingStatus.update(payload));
        registrar.playToClient(io.github.dirnei.mc.servus.storage.field.FieldWorkerPayload.TYPE,
                io.github.dirnei.mc.servus.storage.field.FieldWorkerPayload.STREAM_CODEC,
                (payload, context) -> io.github.dirnei.mc.servus.storage.client.ClientFieldWorker.update(payload));
        registrar.playToClient(FieldCropStockPayload.TYPE, FieldCropStockPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player().containerMenu instanceof FieldHeadMenu menu) {
                        menu.acceptStock(payload);
                    }
                });
        registrar.playToServer(ChooseFieldCropPayload.TYPE, ChooseFieldCropPayload.STREAM_CODEC,
                (payload, context) -> FieldHeadMenu.choose(context.player(), payload.crop()));
        registrar.playToClient(FieldFertilizerStockPayload.TYPE, FieldFertilizerStockPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player().containerMenu instanceof FieldHeadMenu menu) {
                        menu.acceptFertilizerStock(payload);
                    }
                });
        registrar.playToServer(ChooseFertilizerPayload.TYPE, ChooseFertilizerPayload.STREAM_CODEC,
                (payload, context) -> FieldHeadMenu.chooseFertilizer(context.player(), payload.fertilizer()));
        registrar.playToServer(ShuttleBuildStepPayload.TYPE, ShuttleBuildStepPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof ShuttleWorkshopMenu menu) {
                        menu.reportStep(payload.step());
                    }
                });
        registrar.playToServer(EngraveShuttlePayload.TYPE, EngraveShuttlePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof ShuttleWorkshopMenu menu) {
                        menu.engrave(payload.name());
                    }
                });
        registrar.playToServer(KeepSlotPayload.TYPE, KeepSlotPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof OutboundStationMenu menu) {
                        menu.setKeep(payload.slot(), payload.item(), payload.amount());
                    }
                });
        registrar.playToServer(SetLabelNamePayload.TYPE, SetLabelNamePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof ZoneLabelMenu menu) {
                        menu.setName(payload.name());
                    }
                });
        registrar.playToServer(SetLabelTagsPayload.TYPE, SetLabelTagsPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof ZoneLabelMenu menu) {
                        menu.setTags(payload.line());
                    }
                });
        registrar.playToServer(RequestItemPayload.TYPE, RequestItemPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof StorageTerminalMenu menu) {
                        menu.request(ItemKey.of(payload.item()), payload.amount());
                    }
                });
        registrar.playToServer(CraftPreviewRequestPayload.TYPE, CraftPreviewRequestPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof StorageTerminalMenu menu) {
                        PacketDistributor.sendToPlayer(player, menu.preview(ItemKey.of(payload.item()), payload.amount()));
                    }
                });
        registrar.playToClient(CraftPreviewPayload.TYPE, CraftPreviewPayload.STREAM_CODEC,
                (payload, context) -> ClientStock.preview(payload));
        registrar.playToServer(CraftStartPayload.TYPE, CraftStartPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof StorageTerminalMenu menu) {
                        menu.craft(ItemKey.of(payload.item()), payload.amount());
                    }
                });
        registrar.playToServer(SetStockRulePayload.TYPE, SetStockRulePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof StorageTerminalMenu menu) {
                        menu.setRule(ItemKey.of(payload.item()), payload.min(), payload.max());
                    }
                });
        registrar.playToServer(CancelRequestPayload.TYPE, CancelRequestPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof StorageTerminalMenu menu) {
                        menu.cancel(payload.id());
                    }
                });
        registrar.playToServer(PushCarriedPayload.TYPE, PushCarriedPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof StorageTerminalMenu menu) {
                        menu.pushCarried();
                    }
                });
        registrar.playToServer(GridPullPayload.TYPE, GridPullPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof StorageTerminalMenu menu && !payload.item().isEmpty()) {
                        menu.pullToCursor(ItemKey.of(payload.item()));
                    }
                });
        registrar.playToServer(GridClearPayload.TYPE, GridClearPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof StorageTerminalMenu menu) {
                        menu.clearGrid();
                    }
                });
        registrar.playToServer(GridFillPayload.TYPE, GridFillPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof StorageTerminalMenu menu) {
                        menu.fillGrid(payload.slots(), payload.max());
                    }
                });
    }
}
