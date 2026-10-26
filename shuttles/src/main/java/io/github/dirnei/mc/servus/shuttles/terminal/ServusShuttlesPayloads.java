package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.client.ClientFallingTrees;
import io.github.dirnei.mc.servus.shuttles.client.ClientFieldWorker;
import io.github.dirnei.mc.servus.shuttles.client.ClientLifts;
import io.github.dirnei.mc.servus.shuttles.client.ClientPackingStatus;
import io.github.dirnei.mc.servus.shuttles.client.ClientProduction;
import io.github.dirnei.mc.servus.shuttles.client.ClientShuttles;
import io.github.dirnei.mc.servus.shuttles.client.ClientSiteStatus;
import io.github.dirnei.mc.servus.shuttles.client.ClientStationState;
import io.github.dirnei.mc.servus.shuttles.client.ClientStock;
import io.github.dirnei.mc.servus.shuttles.controller.RenameSitePayload;
import io.github.dirnei.mc.servus.shuttles.outbound.ClaimExportPayload;
import io.github.dirnei.mc.servus.shuttles.outbound.ExportListPayload;
import io.github.dirnei.mc.servus.shuttles.controller.SiteStatusPayload;
import io.github.dirnei.mc.servus.shuttles.field.ChooseAntiCorrosionPayload;
import io.github.dirnei.mc.servus.shuttles.field.ChooseFertilizerPayload;
import io.github.dirnei.mc.servus.shuttles.field.ChooseFieldCropPayload;
import io.github.dirnei.mc.servus.shuttles.field.ChooseFieldKindPayload;
import io.github.dirnei.mc.servus.shuttles.field.FieldKind;
import io.github.dirnei.mc.servus.shuttles.field.FieldAmount;
import io.github.dirnei.mc.servus.shuttles.field.FieldCropStockPayload;
import io.github.dirnei.mc.servus.shuttles.field.FieldSupplyStockPayload;
import io.github.dirnei.mc.servus.shuttles.field.FieldWorkerPayload;
import io.github.dirnei.mc.servus.shuttles.field.SetFieldAmountPayload;
import io.github.dirnei.mc.servus.shuttles.field.TreeFallPayload;
import io.github.dirnei.mc.servus.shuttles.gantry.SetGantryLockPayload;
import io.github.dirnei.mc.servus.shuttles.garage.GarageStrandedPayload;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLocksPayload;
import io.github.dirnei.mc.servus.shuttles.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.shuttles.menu.FluidGantryMenu;
import io.github.dirnei.mc.servus.shuttles.menu.LockedMenu;
import io.github.dirnei.mc.servus.shuttles.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.outbound.KeepSlotPayload;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStatusPayload;
import io.github.dirnei.mc.servus.shuttles.pattern.OpenRecipeEditorPayload;
import io.github.dirnei.mc.servus.shuttles.production.CancelProductionOrderPayload;
import io.github.dirnei.mc.servus.shuttles.production.EditPatternPayload;
import io.github.dirnei.mc.servus.shuttles.production.ProductionPayload;
import io.github.dirnei.mc.servus.shuttles.shuttle.LiftSyncPayload;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleSyncPayload;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.EngraveShuttlePayload;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleBuildStepPayload;
import io.github.dirnei.mc.servus.shuttles.station.StationStatePayload;
import io.github.dirnei.mc.servus.shuttles.zone.SetLabelNamePayload;
import io.github.dirnei.mc.servus.shuttles.zone.SetLabelTagsPayload;
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

/** Registers the Shuttles payloads. */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID)
public final class ServusShuttlesPayloads {

    private ServusShuttlesPayloads() {
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
                (payload, context) -> FieldHeadMenu.choose(context.player(), payload.crop(), payload.twoByTwo()));
        registrar.playToServer(ChooseFieldKindPayload.TYPE, ChooseFieldKindPayload.STREAM_CODEC,
                (payload, context) -> FieldHeadMenu.chooseKind(context.player(), FieldKind.byId(payload.kind())));
        registrar.playToServer(ChooseFertilizerPayload.TYPE, ChooseFertilizerPayload.STREAM_CODEC,
                (payload, context) -> FieldHeadMenu.chooseFertilizer(context.player(), payload.fertilizer()));
        registrar.playToServer(ChooseAntiCorrosionPayload.TYPE, ChooseAntiCorrosionPayload.STREAM_CODEC,
                (payload, context) -> FieldHeadMenu.chooseAntiCorrosion(context.player(), payload.item()));
        registrar.playToServer(SetFieldAmountPayload.TYPE, SetFieldAmountPayload.STREAM_CODEC,
                (payload, context) -> FieldHeadMenu.setAmount(context.player(), payload.pos(), FieldAmount.byId(payload.which()), payload.value()));
        toMenu(registrar, ShuttleBuildStepPayload.TYPE, ShuttleBuildStepPayload.STREAM_CODEC, ShuttleWorkshopMenu.class,
                (player, menu, payload) -> menu.reportStep(payload.step()));
        toMenu(registrar, EngraveShuttlePayload.TYPE, EngraveShuttlePayload.STREAM_CODEC, ShuttleWorkshopMenu.class,
                (player, menu, payload) -> menu.engrave(payload.name()));
        toMenu(registrar, KeepSlotPayload.TYPE, KeepSlotPayload.STREAM_CODEC, OutboundStationMenu.class,
                (player, menu, payload) -> menu.setKeep(payload.slot(), payload.item(), payload.amount()));
        toMenu(registrar, ClaimExportPayload.TYPE, ClaimExportPayload.STREAM_CODEC, OutboundStationMenu.class,
                (player, menu, payload) -> menu.claim(payload.slot(), payload.item()));
        toMenu(registrar, RenameSitePayload.TYPE, RenameSitePayload.STREAM_CODEC, WarehouseControllerMenu.class,
                (player, menu, payload) -> menu.rename(payload.name()));
        toMenu(registrar, SetLabelNamePayload.TYPE, SetLabelNamePayload.STREAM_CODEC, ZoneLabelMenu.class,
                (player, menu, payload) -> menu.setName(payload.name()));
        toMenu(registrar, SetLabelTagsPayload.TYPE, SetLabelTagsPayload.STREAM_CODEC, ZoneLabelMenu.class,
                (player, menu, payload) -> menu.setTags(payload.line()));
        toMenu(registrar, SetGantryLockPayload.TYPE, SetGantryLockPayload.STREAM_CODEC, FluidGantryMenu.class,
                (player, menu, payload) -> menu.setLock(payload.tank(), payload.kind(), payload.fluid()));
        registerTerminalRequests(registrar);
    }

    private static void registerClientBound(PayloadRegistrar registrar) {
        registrar.playToClient(StockSyncPayload.TYPE, StockSyncPayload.STREAM_CODEC, (payload, context) -> ClientStock.update(payload));
        registrar.playToClient(ProductionPayload.TYPE, ProductionPayload.STREAM_CODEC, (payload, context) -> ClientProduction.update(payload));
        registrar.playToClient(ShuttleSyncPayload.TYPE, ShuttleSyncPayload.STREAM_CODEC, (payload, context) -> ClientShuttles.update(payload));
        registrar.playToClient(LiftSyncPayload.TYPE, LiftSyncPayload.STREAM_CODEC, (payload, context) -> ClientLifts.update(payload));
        registrar.playToClient(StationStatePayload.TYPE, StationStatePayload.STREAM_CODEC,
                (payload, context) -> ClientStationState.update(payload));
        registrar.playToClient(ExportListPayload.TYPE, ExportListPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof OutboundStationMenu menu) {
                menu.acceptExports(payload);
            }
        });
        registrar.playToClient(PackingStatusPayload.TYPE, PackingStatusPayload.STREAM_CODEC,
                (payload, context) -> ClientPackingStatus.update(payload));
        registrar.playToClient(FieldWorkerPayload.TYPE, FieldWorkerPayload.STREAM_CODEC, (payload, context) -> ClientFieldWorker.update(payload));
        registrar.playToClient(TreeFallPayload.TYPE, TreeFallPayload.STREAM_CODEC, (payload, context) -> ClientFallingTrees.accept(payload));
        registrar.playToClient(FieldCropStockPayload.TYPE, FieldCropStockPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof FieldHeadMenu menu) {
                menu.acceptStock(payload);
            }
        });
        registrar.playToClient(FieldSupplyStockPayload.TYPE, FieldSupplyStockPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof FieldHeadMenu menu) {
                menu.acceptSupplyStock(payload);
            }
        });
        registrar.playToClient(SiteStatusPayload.TYPE, SiteStatusPayload.STREAM_CODEC, (payload, context) -> ClientSiteStatus.update(payload));
        registrar.playToClient(CraftPreviewPayload.TYPE, CraftPreviewPayload.STREAM_CODEC, (payload, context) -> ClientStock.preview(payload));
        registrar.playToClient(SlotLocksPayload.TYPE, SlotLocksPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof LockedMenu menu && context.player().containerMenu.containerId == payload.containerId()) {
                menu.locks().accept(payload.entries());
            }
        });
        registrar.playToClient(GarageStrandedPayload.TYPE, GarageStrandedPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof ShuttleGarageMenu menu && menu.containerId == payload.containerId()) {
                menu.acceptStranded(payload);
            }
        });
    }

    /** What the Storage Terminal screen sends: requests, crafts, rules, sites and the order grid. */
    private static void registerTerminalRequests(PayloadRegistrar registrar) {
        toMenu(registrar, RequestItemPayload.TYPE, RequestItemPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.request(ItemKey.of(payload.item()), payload.amount()));
        toMenu(registrar, CraftPreviewRequestPayload.TYPE, CraftPreviewRequestPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> PacketDistributor.sendToPlayer(player, menu.preview(payload.item(), payload.amount(), payload.mode())));
        toMenu(registrar, CraftStartPayload.TYPE, CraftStartPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.craft(ItemKey.of(payload.item()), payload.amount()));
        toMenu(registrar, OrderStartPayload.TYPE, OrderStartPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.startOrder(payload.amount()));
        toMenu(registrar, OrderSavePayload.TYPE, OrderSavePayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.savePattern());
        toMenu(registrar, SetStockRulePayload.TYPE, SetStockRulePayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.setRule(ItemKey.of(payload.item()), payload.min(), payload.max(), payload.cap(),
                        payload.export(), payload.remove()));
        toMenu(registrar, SelectSitePayload.TYPE, SelectSitePayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.selectSite(payload.site()));
        toMenu(registrar, CancelRequestPayload.TYPE, CancelRequestPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.cancel(payload.id()));
        toMenu(registrar, PushCarriedPayload.TYPE, PushCarriedPayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.pushCarried());
        toMenu(registrar, OrderGridRecipePayload.TYPE, OrderGridRecipePayload.STREAM_CODEC, StorageTerminalMenu.class,
                (player, menu, payload) -> menu.fillFromRecipe(payload.recipe(), payload.keepMode()));
    }
}
