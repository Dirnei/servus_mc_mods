package io.github.dirnei.mc.servus.foundation.machine.loader;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

/** The ticket controller chunk loaders force chunks with. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class ChunkLoaderTickets {

    public static final TicketController CONTROLLER =
            new TicketController(ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "chunk_loader"));

    private ChunkLoaderTickets() {
    }

    @SubscribeEvent
    public static void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }
}
