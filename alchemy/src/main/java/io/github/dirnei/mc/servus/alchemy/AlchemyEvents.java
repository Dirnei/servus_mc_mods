package io.github.dirnei.mc.servus.alchemy;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTierData;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeGiftData;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinData;
import io.github.dirnei.mc.servus.alchemy.sight.ElixirDoseData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/** Game bus hooks of the alchemy: data that reloads with {@code /reload}. */
@EventBusSubscriber(modid = ServusAlchemyIds.MOD_ID)
public final class AlchemyEvents {

    private AlchemyEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(AthanorTierData.LISTENER_ID, AthanorTierData.INSTANCE);
        event.addListener(VeinData.LISTENER_ID, VeinData.INSTANCE);
        event.addListener(GnomeGiftData.LISTENER_ID, GnomeGiftData.INSTANCE);
        event.addListener(ElixirDoseData.LISTENER_ID, ElixirDoseData.INSTANCE);
    }
}
