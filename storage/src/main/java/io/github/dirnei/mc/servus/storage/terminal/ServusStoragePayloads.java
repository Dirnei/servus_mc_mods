package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.client.ClientStock;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.DispatchStationMenu;
import io.github.dirnei.mc.servus.storage.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.storage.zone.SetLabelNamePayload;
import io.github.dirnei.mc.servus.storage.zone.SetLabelTagsPayload;
import io.github.dirnei.mc.servus.storage.client.ClientZoneRules;
import io.github.dirnei.mc.servus.storage.station.ZoneRulesSyncPayload;
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
        registrar.playToClient(ShuttleSyncPayload.TYPE, ShuttleSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientShuttles.update(payload));
        registrar.playToServer(PullItemPayload.TYPE, PullItemPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof CounterTerminalMenu menu) {
                        menu.pull(ItemKey.of(payload.item()), payload.toInventory());
                    }
                });
        registrar.playToClient(ZoneRulesSyncPayload.TYPE, ZoneRulesSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientZoneRules.update(payload));
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
        registrar.playToServer(PushCarriedPayload.TYPE, PushCarriedPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof CounterTerminalMenu menu) {
                        menu.pushCarried();
                    }
                });
    }
}
