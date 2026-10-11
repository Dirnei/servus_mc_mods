package io.github.dirnei.mc.servus.storage.site;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.ArrayList;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

/**
 * The chunk tickets of site wakes. A wake never survives a restart: when a
 * world loads, the validation callback drops every ticket this controller saved.
 */
@EventBusSubscriber(modid = ServusStorageIds.MOD_ID)
public final class SiteTickets {

    public static final TicketController CONTROLLER = new TicketController(
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "site_wake"),
            (level, helper) -> new ArrayList<>(helper.getBlockTickets().keySet()).forEach(helper::removeAllTickets));

    private SiteTickets() {
    }

    @SubscribeEvent
    public static void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }
}
