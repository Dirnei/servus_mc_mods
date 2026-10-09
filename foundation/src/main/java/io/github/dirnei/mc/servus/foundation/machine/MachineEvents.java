package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Event hooks of the shared machine base and the machines Foundation owns. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class MachineEvents {

    private MachineEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MachineStatsManager.INSTANCE);
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
        public static void onRegisterGameTests(RegisterGameTestsEvent event) {
            event.register(MachineGameTests.class);
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MachineContent.COAL_GENERATOR_ENTITY.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MachineContent.COAL_GENERATOR_ENTITY.get(),
                    (machine, side) -> machine.energyStorage());
            // Burner machines keep their fuel slot stocked from the data line.
            event.registerBlockEntity(ServusFoundationCapabilities.UPKEEP, MachineContent.COAL_GENERATOR_ENTITY.get(),
                    (machine, side) -> machine.upkeep());
        }
    }
}
