package io.github.dirnei.mc.servus.manufacturing;

import io.github.dirnei.mc.servus.lib.registry.ServusLibCreativeTabs;
import io.github.dirnei.mc.servus.manufacturing.deposit.DepositManager;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
    }

    /** Mod bus hooks. */
    @EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(ServusLibCreativeTabs.SERVUS_KEY)) {
                event.accept(ServusManufacturingItems.IRON_DEPOSIT);
                event.accept(ServusManufacturingItems.COPPER_DEPOSIT);
                event.accept(ServusManufacturingItems.COAL_DEPOSIT);
                event.accept(ServusManufacturingItems.STONE_DEPOSIT);
            }
        }
    }
}
