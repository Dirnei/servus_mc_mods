package io.github.dirnei.mc.servus.foundation.logistics;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctStatsManager;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeItem;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlockEntities;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankStatsManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Event hooks of Servus: Logistics. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class ServusLogisticsEvents {

    private ServusLogisticsEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(DuctStatsManager.LISTENER_ID, DuctStatsManager.INSTANCE);
        event.addListener(TankStatsManager.LISTENER_ID, TankStatsManager.INSTANCE);
    }

    /** Mod bus hooks. */
    @EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(ServusFoundationCreativeTabs.SERVUS_KEY)) {
                ServusLogisticsItems.CREATIVE_TAB_ORDER.forEach(event::accept);
                FacadeItem.examples().forEach(event::accept);
                FacadeItem.hollowExamples().forEach(event::accept);
            }
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.Item.BLOCK, ServusLogisticsBlockEntities.UTILITY_DUCT.get(),
                    (duct, side) -> duct.itemHandler(side));
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, ServusLogisticsBlockEntities.FLUID_TANK.get(),
                    (tank, side) -> tank.fluidHandler());
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, ServusLogisticsBlockEntities.UTILITY_DUCT.get(),
                    (duct, side) -> duct.fluidHandler(side));
        }
    }
}
