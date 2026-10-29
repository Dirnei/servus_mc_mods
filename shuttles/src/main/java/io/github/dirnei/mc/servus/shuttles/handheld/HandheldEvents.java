package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Forgets a player's handheld sweep when they leave. */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID)
public final class HandheldEvents {

    private HandheldEvents() {
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HandheldActions.forget(event.getEntity().getUUID());
    }
}
