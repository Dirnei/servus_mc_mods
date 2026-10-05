package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.client.ClientStock;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
        registrar.playToServer(PullItemPayload.TYPE, PullItemPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof CounterTerminalMenu menu) {
                        menu.pull(ItemKey.of(payload.item()), payload.toInventory());
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
