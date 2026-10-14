package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.client.ClientFieldWorker;
import io.github.dirnei.mc.servus.storage.client.ClientLifts;
import io.github.dirnei.mc.servus.storage.client.ClientPackingStatus;
import io.github.dirnei.mc.servus.storage.client.ClientProduction;
import io.github.dirnei.mc.servus.storage.client.ClientShuttles;
import io.github.dirnei.mc.servus.storage.client.ClientSiteStatus;
import io.github.dirnei.mc.servus.storage.client.ClientStationState;
import io.github.dirnei.mc.servus.storage.client.ClientStock;
import io.github.dirnei.mc.servus.storage.controller.RenameSitePayload;
import io.github.dirnei.mc.servus.storage.controller.SiteStatusPayload;
import io.github.dirnei.mc.servus.storage.field.ChooseFertilizerPayload;
import io.github.dirnei.mc.servus.storage.field.ChooseFieldCropPayload;
import io.github.dirnei.mc.servus.storage.field.FieldAmount;
import io.github.dirnei.mc.servus.storage.field.FieldCropStockPayload;
import io.github.dirnei.mc.servus.storage.field.FieldFertilizerStockPayload;
import io.github.dirnei.mc.servus.storage.field.SetFieldAmountPayload;
import io.github.dirnei.mc.servus.storage.field.FieldWorkerPayload;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.storage.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.storage.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.outbound.KeepSlotPayload;
import io.github.dirnei.mc.servus.storage.packing.PackingStatusPayload;
import io.github.dirnei.mc.servus.storage.pattern.OpenRecipeEditorPayload;
import io.github.dirnei.mc.servus.storage.production.CancelProductionOrderPayload;
import io.github.dirnei.mc.servus.storage.production.EditPatternPayload;
import io.github.dirnei.mc.servus.storage.production.ProductionPayload;
import io.github.dirnei.mc.servus.storage.shuttle.LiftSyncPayload;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleSyncPayload;
import io.github.dirnei.mc.servus.storage.shuttle.module.EngraveShuttlePayload;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleBuildStepPayload;
import io.github.dirnei.mc.servus.storage.station.StationStatePayload;
import io.github.dirnei.mc.servus.storage.zone.SetLabelNamePayload;
import io.github.dirnei.mc.servus.storage.zone.SetLabelTagsPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers the Storage payloads. */
@EventBusSubscriber(modid = ServusStorageIds.MOD_ID)
public final class ServusStoragePayloads {

    private ServusStoragePayloads() {
    }

    /** What a client to server payload does with the sender's open menu. */
    @FunctionalInterface
    private interface MenuHandler<M, T> {
        void handle(ServerPlayer player, M menu, T payload);
    }

    /** Registers a client to server payload that acts only while the sender's open menu is of {@code menuType}. */
    private static <T extends CustomPacketPayload, M extends AbstractContainerMenu> void toMenu(PayloadRegistrar registrar,
            CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Class<M> menuType,
            MenuHandler<M, T> handler) {
        registrar.playToServer(type, codec, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player && menuType.isInstance(player.containerMenu)) {
                handler.handle(player, menuType.cast(player.containerMenu), payload);
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registerClientBound(registrar);
        toMenu(registrar, CancelProductionOrderPayload.TYPE, CancelProductionOrderPayload.STREAM_CODEC, ProductionTerminalMenu.class,
                (player, menu, payload) -> menu.cancel(payload.cpu(), payload.id()));
        toMenu(registrar, OpenRecipeEditorPayload.TYPE, OpenRecipeEditorPayload.STREAM_CODEC, ProductionTerminalMenu.class,
                (player, menu, payload) -> menu.openEditorWith(payload.recipe()));
        toMenu(registrar, EditPatternPayload.TYPE, EditPatternPayload.STREAM_CODEC, ProductionTerminalMenu.class,
                (player, menu, payload) -> menu.openExisting(payload.storage(), payload.slot()));
        registrar.playToServer(ChooseFieldCropPayload.TYPE, ChooseFieldCropPayload.STREAM_CODEC,
                (payload, context) -> FieldHeadMenu.choose(context.player(), payload.crop()));
        registrar.playToServer(ChooseFertilizerPayload.TYPE, ChooseFertilizerPayload.STREAM_CODEC,
                (payload, context) -> FieldHeadMenu.chooseFertilizer(context.player(), payload.fertilizer()));
        registrar.playToServer(SetFieldAmountPayload.TYPE, SetFieldAmountPayload.STREAM_CODEC,
                (payload, context) -> FieldHeadMenu.setAmount(context.player(), payload.pos(), FieldAmount.byId(payload.which()), payload.value()));
        toMenu(registrar, ShuttleBuildStepPayload.TYPE, ShuttleBuildStepPayload.STREAM_CODEC, ShuttleWorkshopMenu.class,
                (player, menu, payload) -> menu.reportStep(payload.step()));
        toMenu(registrar, EngraveShuttlePayload.TYPE, EngraveShuttlePayload.STREAM_CODEC, ShuttleWorkshopMenu.class,
                (player, menu, payload) -> menu.engrave(payload.name()));
        toMenu(registrar, KeepSlotPayload.TYPE, KeepSlotPayload.STREAM_CODEC, OutboundStationMenu.class,
                (player, menu, payload) -> menu.setKeep(payload.slot(), payload.item(), payload.amount()));
        toMenu(registrar, RenameSitePayload.TYPE, RenameSitePayload.STREAM_CODEC, WarehouseControllerMenu.class,
                (player, menu, payload) -> menu.rename(payload.name()));
        toMenu(registrar, SetLabelNamePayload.TYPE, SetLabelNamePayload.STREAM_CODEC, ZoneLabelMenu.class,
                (player, menu, payload) -> menu.setName(payload.name()));
        toMenu(registrar, SetLabelTagsPayload.TYPE, SetLabelTagsPayload.STREAM_CODEC, ZoneLabelMenu.class,
                (player, menu, payload) -> menu.setTags(payload.line()));
        registerTerminalRequests(registrar);
    }

    private static void registerClientBound(PayloadRegistrar registrar) {
        registrar.playToClient(StockSyncPayload.TYPE, StockSyncPayload.STREAM_CODEC, (payload, context) -> ClientStock.update(payload));
        registrar.playToClient(ProductionPayload.TYPE, ProductionPayload.STREAM_CODEC, (payload, context) -> ClientProduction.update(payload));
        registrar.playToClient(ShuttleSyncPayload.TYPE, ShuttleSyncPayload.STREAM_CODEC, (payload, context) -> ClientShuttles.update(payload));
        registrar.playToClient(LiftSyncPayload.TYPE, LiftSyncPayload.STREAM_CODEC, (payload, context) -> ClientLifts.update(payload));
        registrar.playToClient(StationStatePayload.TYPE, StationStatePayload.STREAM_CODEC,
                (payload, context) -> ClientStationState.update(payload));
        registrar.playToClient(PackingStatusPayload.TYPE, PackingStatusPayload.STREAM_CODEC,
                (payload, context) -> ClientPackingStatus.update(payload));
        registrar.playToClient(FieldWorkerPayload.TYPE, FieldWorkerPayload.STREAM_CODEC, (payload, context) -> ClientFieldWorker.update(payload));
        registrar.playToClient(FieldCropStockPayload.TYPE, FieldCropStockPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof FieldHeadMenu menu) {
                menu.acceptStock(payload);
            }
        });
        registrar.playToClient(FieldFertilizerStockPayload.TYPE, FieldFertilizerStockPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof FieldHeadMenu menu) {
                menu.acceptFertilizerStock(payload);
            }
        });
        registrar.playToClient(SiteStatusPayload.TYPE, SiteStatusPayload.STREAM_CODEC, (payload, context) -> ClientSiteStatus.update(payload));
        registrar.playToClient(CraftPreviewPayload.TYPE, CraftPreviewPayload.STREAM_CODEC, (payload, context) -> ClientStock.preview(payload));
    }

    /** What the Storage Terminal screen sends: requests, crafts, rules, sites and the crafting grid. */
    private static void registerTerminalRequests(PayloadRegistrar registrar) {
        toMenu(registrar, RequestItemPayload.TYPE, RequestItemPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.request(ItemKey.of(payload.item()), payload.amount()));
        toMenu(registrar, CraftPreviewRequestPayload.TYPE, CraftPreviewRequestPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> PacketDistributor.sendToPlayer(player, menu.preview(ItemKey.of(payload.item()), payload.amount())));
        toMenu(registrar, CraftStartPayload.TYPE, CraftStartPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.craft(ItemKey.of(payload.item()), payload.amount()));
        toMenu(registrar, SetStockRulePayload.TYPE, SetStockRulePayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.setRule(ItemKey.of(payload.item()), payload.min(), payload.max()));
        toMenu(registrar, SelectSitePayload.TYPE, SelectSitePayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.selectSite(payload.site()));
        toMenu(registrar, CancelRequestPayload.TYPE, CancelRequestPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.cancel(payload.id()));
        toMenu(registrar, PushCarriedPayload.TYPE, PushCarriedPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.pushCarried());
        toMenu(registrar, GridClearPayload.TYPE, GridClearPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.clearGrid());
        toMenu(registrar, GridFillPayload.TYPE, GridFillPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.fillGrid(payload.slots(), payload.max()));
    }
}
