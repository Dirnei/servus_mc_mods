package io.github.dirnei.mc.servus.brewery;

import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Mod bus hooks of Servus: Brewery. */
@EventBusSubscriber(modid = ServusBreweryIds.MOD_ID)
public final class ServusBreweryEvents {

    private ServusBreweryEvents() {
    }

    /** The Malting Floor's slot is its item handler on every side. */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ServusBreweryBlockEntities.MALTING_FLOOR.get(),
                (floor, side) -> floor.slot());
    }
}
