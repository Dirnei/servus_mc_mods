package io.github.dirnei.mc.servus.brewery;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseStatsManager;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlockEntities;
import io.github.dirnei.mc.servus.delicacies.firebox.DelicaciesCapabilities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/** Capabilities and reload listeners of Servus: Brewery. */
@EventBusSubscriber(modid = ServusBreweryIds.MOD_ID)
public final class ServusBreweryEvents {

    private ServusBreweryEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(BrewhouseStatsManager.LISTENER_ID, BrewhouseStatsManager.INSTANCE);
    }

    /** The Malting Floor's slot on every side; the Brewhouse parts offer the firebox, items and fluids of their cell. */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ServusBreweryBlockEntities.MALTING_FLOOR.get(),
                (floor, side) -> floor.slot());
        event.registerBlockEntity(DelicaciesCapabilities.FIREBOX, ServusBreweryBlockEntities.BREWHOUSE.get(),
                (part, context) -> part.firebox());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ServusBreweryBlockEntities.BREWHOUSE.get(),
                (part, side) -> part.itemCapability());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ServusBreweryBlockEntities.BREWHOUSE.get(),
                (part, side) -> part.fluidCapability());
    }
}
