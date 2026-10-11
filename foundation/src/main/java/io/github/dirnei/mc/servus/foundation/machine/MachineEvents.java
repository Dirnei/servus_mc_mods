package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkStatsManager;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Event hooks of the shared machine base and the machines Foundation owns. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class MachineEvents {

    private MachineEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(MachineStatsManager.LISTENER_ID, MachineStatsManager.INSTANCE);
        event.addListener(SinkStatsManager.LISTENER_ID, SinkStatsManager.INSTANCE);
    }

    /** Mod bus hooks. */
    @EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(ServusFoundationCreativeTabs.SERVUS_KEY)) {
                MachineContent.creativeTabOrder().forEach(event::accept);
            }
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.Item.BLOCK, MachineContent.COAL_GENERATOR_ENTITY.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.Energy.BLOCK, MachineContent.COAL_GENERATOR_ENTITY.get(),
                    (machine, side) -> machine.energyStorage());
            // Burner machines keep their fuel slot stocked from the data line.
            event.registerBlockEntity(ServusFoundationCapabilities.UPKEEP, MachineContent.COAL_GENERATOR_ENTITY.get(),
                    (machine, side) -> machine.upkeep());
            event.registerBlockEntity(ServusFoundationCapabilities.UPKEEP, MachineContent.BURNER_PUMP_ENTITY.get(),
                    (machine, side) -> machine.upkeep());
            event.registerBlockEntity(Capabilities.Item.BLOCK, MachineContent.BURNER_PUMP_ENTITY.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, MachineContent.BURNER_PUMP_ENTITY.get(),
                    (machine, side) -> machine.fluidHandler(side));
            // A pump takes energy only where it also gives water: not through its front or bottom.
            event.registerBlockEntity(Capabilities.Energy.BLOCK, MachineContent.ELECTRIC_PUMP_ENTITY.get(),
                    (machine, side) -> side == null || PumpBlock.givesWater(machine.getBlockState(), side) ? machine.energyStorage() : null);
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, MachineContent.ELECTRIC_PUMP_ENTITY.get(),
                    (machine, side) -> machine.fluidHandler(side));
            event.registerBlockEntity(Capabilities.Energy.BLOCK, MachineContent.CHUNK_LOADER_ENTITY.get(),
                    (machine, side) -> machine.energyStorage());
            // Every formed Sink block hands out its controller's handlers, except on the front faces; casings offer none.
            event.registerBlockEntity(Capabilities.Item.BLOCK, MachineContent.SINK_ENTITY.get(),
                    (sink, side) -> sink.itemHandler(side));
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, MachineContent.SINK_ENTITY.get(),
                    (sink, side) -> sink.fluidHandler(side));
            event.registerBlockEntity(Capabilities.Energy.BLOCK, MachineContent.SINK_ENTITY.get(),
                    (sink, side) -> sink.energyStorage(side));
        }
    }
}
