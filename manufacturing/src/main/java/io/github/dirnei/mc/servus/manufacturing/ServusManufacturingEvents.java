package io.github.dirnei.mc.servus.manufacturing;

import io.github.dirnei.mc.servus.lib.registry.ServusLibCreativeTabs;
import io.github.dirnei.mc.servus.manufacturing.deposit.DepositManager;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineStatsManager;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Game bus hooks of Servus: Manufacturing. */
@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID)
public final class ServusManufacturingEvents {

    private ServusManufacturingEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(DepositManager.INSTANCE);
        event.addListener(MachineStatsManager.INSTANCE);
    }

    /** Mod bus hooks. */
    @EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(ServusLibCreativeTabs.SERVUS_KEY)) {
                ServusManufacturingItems.CREATIVE_TAB_ORDER.forEach(event::accept);
            }
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.BURNER_EXTRACTOR.get(),
                    (machine, side) -> machine.sideHandler());
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.BURNER_SMELTER.get(),
                    (machine, side) -> machine.sideHandler());
        }
    }
}
