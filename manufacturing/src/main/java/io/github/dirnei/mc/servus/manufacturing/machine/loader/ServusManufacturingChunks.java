package io.github.dirnei.mc.servus.manufacturing.machine.loader;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

/** The ticket controller chunk loaders force chunks with. */
@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID)
public final class ServusManufacturingChunks {

    public static final TicketController CONTROLLER =
            new TicketController(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "chunk_loader"));

    private ServusManufacturingChunks() {
    }

    @SubscribeEvent
    public static void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }
}
