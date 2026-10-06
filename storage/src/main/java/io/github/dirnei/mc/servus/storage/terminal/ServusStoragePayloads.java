package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.client.ClientStock;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.DispatchStationMenu;
import io.github.dirnei.mc.servus.storage.client.ClientZoneRules;
import io.github.dirnei.mc.servus.storage.station.SelectZonePayload;
import io.github.dirnei.mc.servus.storage.station.SetOverflowPayload;
import io.github.dirnei.mc.servus.storage.station.SetZoneTagsPayload;
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
        registrar.playToServer(SelectZonePayload.TYPE, SelectZonePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof DispatchStationMenu menu) {
                        menu.selectZone(payload.key());
                    }
                });
        registrar.playToServer(SetZoneTagsPayload.TYPE, SetZoneTagsPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof DispatchStationMenu menu) {
                        menu.setTags(payload.line());
                    }
                });
        registrar.playToServer(SetOverflowPayload.TYPE, SetOverflowPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof DispatchStationMenu menu) {
                        menu.setOverflow(payload.overflow());
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
