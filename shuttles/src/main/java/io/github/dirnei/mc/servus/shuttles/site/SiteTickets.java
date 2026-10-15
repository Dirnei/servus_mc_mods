package io.github.dirnei.mc.servus.shuttles.site;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.ArrayList;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

/**
 * The chunk tickets of site wakes. A wake never survives a restart: when a
 * world loads, the validation callback drops every ticket this controller saved.
 */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID)
public final class SiteTickets {

    public static final TicketController CONTROLLER = new TicketController(
            Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "site_wake"),
            (level, helper) -> new ArrayList<>(helper.getBlockTickets().keySet()).forEach(helper::removeAllTickets));

    private SiteTickets() {
    }

    @SubscribeEvent
    public static void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }
}
