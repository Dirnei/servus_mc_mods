package io.github.dirnei.mc.servus.brewery;

import io.github.dirnei.mc.servus.brewery.beer.CellarStatsManager;
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
        event.addListener(CellarStatsManager.LISTENER_ID, CellarStatsManager.INSTANCE);
    }

    /**
     * The Malting Floor's slot on every side; the Brewhouse parts offer the firebox, items and fluids of their cell; the
     * Fermenting Vat its yeast and tank and the Cask its beer on every side.
     */
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
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ServusBreweryBlockEntities.BREWHOUSE.get(),
                (part, side) -> part.energyCapability(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ServusBreweryBlockEntities.FERMENTING_VAT.get(),
                (vat, side) -> vat.yeastSlot());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ServusBreweryBlockEntities.FERMENTING_VAT.get(),
                (vat, side) -> vat.fluids());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ServusBreweryBlockEntities.CASK.get(),
                (cask, side) -> cask.tank());
    }
}
