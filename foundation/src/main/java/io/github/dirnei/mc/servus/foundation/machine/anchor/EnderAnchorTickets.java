package io.github.dirnei.mc.servus.foundation.machine.anchor;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

/** The ticket controller Ender Anchors force chunks with. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class EnderAnchorTickets {

    public static final TicketController CONTROLLER =
            new TicketController(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "ender_anchor"));

    private EnderAnchorTickets() {
    }

    @SubscribeEvent
    public static void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }
}
