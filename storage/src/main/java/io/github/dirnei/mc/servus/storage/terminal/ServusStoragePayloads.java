package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.client.ClientStock;
import io.github.dirnei.mc.servus.storage.client.ClientOrders;
import io.github.dirnei.mc.servus.storage.board.OrderBoardPayload;
import io.github.dirnei.mc.servus.storage.board.CancelOrderPayload;
import io.github.dirnei.mc.servus.storage.menu.OrderBoardMenu;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.storage.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.storage.outbound.KeepSlotPayload;
import io.github.dirnei.mc.servus.storage.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.storage.zone.SetLabelNamePayload;
import io.github.dirnei.mc.servus.storage.zone.SetLabelTagsPayload;
import io.github.dirnei.mc.servus.storage.client.ClientStationState;
import io.github.dirnei.mc.servus.storage.station.StationStatePayload;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirnei.mc.servus.storage.client.ClientShuttles;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleSyncPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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
        registrar.playToClient(OrderBoardPayload.TYPE, OrderBoardPayload.STREAM_CODEC,
                (payload, context) -> ClientOrders.update(payload));
        registrar.playToServer(CancelOrderPayload.TYPE, CancelOrderPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof OrderBoardMenu menu) {
                        menu.cancel(payload.id());
                    }
                });
        registrar.playToClient(ShuttleSyncPayload.TYPE, ShuttleSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientShuttles.update(payload));
        registrar.playToClient(StationStatePayload.TYPE, StationStatePayload.STREAM_CODEC,
                (payload, context) -> ClientStationState.update(payload));
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
                            && player.containerMenu instanceof CounterTerminalMenu menu) {
                        menu.request(ItemKey.of(payload.item()), payload.amount());
                    }
                });
        registrar.playToServer(SetStockRulePayload.TYPE, SetStockRulePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof CounterTerminalMenu menu) {
                        menu.setRule(ItemKey.of(payload.item()), payload.min(), payload.max());
                    }
                });
        registrar.playToServer(CancelRequestPayload.TYPE, CancelRequestPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof CounterTerminalMenu menu) {
                        menu.cancel(payload.id());
                    }
                });
        registrar.playToServer(PushCarriedPayload.TYPE, PushCarriedPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof CounterTerminalMenu menu) {
                        menu.pushCarried();
                    }
                });
    }
}
