package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** Event hooks of the shared machine base and the machines Foundation owns. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class MachineEvents {

    private MachineEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MachineStatsManager.INSTANCE);
    }
}
