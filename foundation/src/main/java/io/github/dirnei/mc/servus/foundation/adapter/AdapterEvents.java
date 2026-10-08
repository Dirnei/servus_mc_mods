package io.github.dirnei.mc.servus.foundation.adapter;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Event hooks of the Machine Adapter. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class AdapterEvents {

    private AdapterEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MachineProfiles.INSTANCE);
    }

    /** Mod bus hooks. */
    @EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(ServusFoundationCreativeTabs.SERVUS_KEY)) {
                event.accept(AdapterContent.ITEM.get());
            }
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, AdapterContent.BLOCK_ENTITY.get(),
                    (adapter, side) -> adapter.production());
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AdapterContent.BLOCK_ENTITY.get(),
                    (adapter, side) -> adapter.itemHandler());
        }
    }
}
