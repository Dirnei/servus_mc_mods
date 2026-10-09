package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorTierData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** Game bus hooks of the alchemy: data that reloads with {@code /reload}. */
@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID)
public final class AlchemyEvents {

    private AlchemyEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(AthanorTierData.INSTANCE);
    }
}
