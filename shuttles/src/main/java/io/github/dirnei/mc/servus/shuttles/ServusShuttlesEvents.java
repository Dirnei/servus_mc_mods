package io.github.dirnei.mc.servus.shuttles;

import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlock;
import io.github.dirnei.mc.servus.shuttles.packing.WarehouseSupplier;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import net.minecraft.network.chat.Component;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Game bus hooks of Servus: Shuttles. */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID)
public final class ServusShuttlesEvents {

    private ServusShuttlesEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ShuttlesStatsManager.LISTENER_ID, ShuttlesStatsManager.INSTANCE);
    }

    /** A garage with shuttles stays put when left clicked. */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (ShuttleGarageBlock.occupied(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            if (!event.getLevel().isClientSide() && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
                event.getEntity().sendOverlayMessage(Component.translatable("message.servus_shuttles.garage.occupied"));
            }
        }
    }

    /** Mod bus hooks. */
    @EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(ServusFoundationCreativeTabs.SERVUS_KEY)) {
                ServusShuttlesItems.CREATIVE_TAB_ORDER.forEach(event::accept);
            }
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, ServusShuttlesBlockEntities.FIELD_HEAD.get(),
                    (head, side) -> head.fluidHandler());
            // Both halves of a fluid station answer for its Fluids layer; the faces between them are inside the block.
            event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, entity, side) -> FluidStationBlock.sideHandler(level, pos, state, side),
                    ServusShuttlesBlocks.FLUID_INBOUND.get(), ServusShuttlesBlocks.FLUID_OUTBOUND.get());
            // A rack is a chest for hoppers and pipes only while no rail docks to its shelf.
            event.registerBlockEntity(Capabilities.Item.BLOCK, ServusShuttlesBlockEntities.HEAVY_RACK.get(),
                    (rack, side) -> rack.standalone() ? rack.items() : null);
            // A Fluid Rack shelf is a tank for pipes only while no rail stands in front of its port.
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, ServusShuttlesBlockEntities.FLUID_RACK.get(),
                    (rack, side) -> rack.standalone() ? rack.shelfHandler() : null);
            event.registerBlockEntity(Capabilities.Item.BLOCK, ServusShuttlesBlockEntities.INBOUND_STATION.get(),
                    (station, side) -> station.handlerFor(side));
            event.registerBlockEntity(ServusFoundationCapabilities.PACKET_ENDPOINT, ServusShuttlesBlockEntities.INBOUND_STATION.get(),
                    (station, side) -> station.endpointFor(side));
            // Packets enter a Packing Station through its inlet; pipes and hoppers see no item capability.
            event.registerBlockEntity(ServusFoundationCapabilities.PACKET_INLET, ServusShuttlesBlockEntities.PACKING_STATION.get(),
                    (point, side) -> point.intake());
            event.registerBlockEntity(ServusFoundationCapabilities.DATA_LINK, ServusShuttlesBlockEntities.PACKING_STATION.get(),
                    (point, side) -> point);
            event.registerBlockEntity(ServusFoundationCapabilities.TRANSFER_GATE, ServusShuttlesBlockEntities.PACKING_STATION.get(),
                    (point, side) -> point);
            event.registerBlockEntity(ServusFoundationCapabilities.PACKET_ENDPOINT, ServusShuttlesBlockEntities.PACKING_STATION.get(),
                    (point, side) -> point);
            event.registerBlockEntity(ServusFoundationCapabilities.DATA_LINK, ServusShuttlesBlockEntities.CRAFTING_CPU.get(),
                    (cpu, side) -> cpu.dataLink());
            // Packets enter the CPU's buffer through its inlet; pipes and hoppers see no item capability.
            event.registerBlockEntity(ServusFoundationCapabilities.PACKET_INLET, ServusShuttlesBlockEntities.CRAFTING_CPU.get(),
                    (cpu, side) -> cpu.packetInlet());
            event.registerBlockEntity(ServusFoundationCapabilities.DATA_LINK, ServusShuttlesBlockEntities.PRODUCTION_TERMINAL.get(),
                    (terminal, side) -> terminal);
            event.registerBlockEntity(ServusFoundationCapabilities.DATA_LINK, ServusShuttlesBlockEntities.PATTERN_STORAGE.get(),
                    (storage, side) -> storage);
            event.registerBlockEntity(ServusFoundationCapabilities.DATA_LINK, ServusShuttlesBlockEntities.STORAGE_TERMINAL.get(),
                    (terminal, side) -> terminal);
            event.registerBlockEntity(ServusFoundationCapabilities.PACKET_INLET, ServusShuttlesBlockEntities.STORAGE_TERMINAL.get(),
                    (terminal, side) -> terminal.packetHandler());
            event.registerBlockEntity(ServusFoundationCapabilities.PACKET_ENDPOINT, ServusShuttlesBlockEntities.STORAGE_TERMINAL.get(),
                    (terminal, side) -> terminal.packetEndpoint());
            event.registerBlockEntity(Capabilities.Item.BLOCK, ServusShuttlesBlockEntities.OUTBOUND_STATION.get(),
                    (station, side) -> station.handlerFor(side));
            event.registerBlockEntity(ServusFoundationCapabilities.PACKET_INLET, ServusShuttlesBlockEntities.CRAFTING_STATION.get(),
                    (station, side) -> station.packetInlet());
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusShuttlesBlockEntities.CRAFTING_STATION.get(),
                    (station, side) -> station.production());
            // The controller's FE buffer: a sink for the power line, never a source.
            event.registerBlockEntity(Capabilities.Energy.BLOCK, ServusShuttlesBlockEntities.WAREHOUSE_CONTROLLER.get(),
                    (controller, side) -> controller.energy());
            event.registerBlockEntity(ServusFoundationCapabilities.STOCK_PROVIDER, ServusShuttlesBlockEntities.WAREHOUSE_CONTROLLER.get(),
                    (controller, side) -> controller.stockProvider());
            event.registerBlockEntity(ServusFoundationCapabilities.SUPPLIER, ServusShuttlesBlockEntities.WAREHOUSE_CONTROLLER.get(),
                    (controller, side) -> new WarehouseSupplier(controller));
            event.registerBlockEntity(ServusFoundationCapabilities.SUPPLIER, ServusShuttlesBlockEntities.PACKING_STATION.get(),
                    (point, side) -> new WarehouseSupplier(point));
        }
    }
}
