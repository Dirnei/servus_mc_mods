package io.github.dirnei.mc.servus.shuttles.site;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.packing.WirelessIncoming;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Game bus hooks of site wakes: tick them, release them before the worlds stop, free stale team slots on start. */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID)
public final class SiteEvents {

    private SiteEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SiteWakes.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SiteWakes.releaseAll(event.getServer());
        WirelessIncoming.clear();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SiteWakes.clearTeamSlots(event.getServer());
    }
}
