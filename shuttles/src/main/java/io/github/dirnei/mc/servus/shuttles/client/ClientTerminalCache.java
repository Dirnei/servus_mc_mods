package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Terminal content cached on the client lives for one session: leaving the world forgets it. */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID, value = Dist.CLIENT)
public final class ClientTerminalCache {

    private ClientTerminalCache() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStock.forgetAll();
        ClientProduction.forgetAll();
    }
}
