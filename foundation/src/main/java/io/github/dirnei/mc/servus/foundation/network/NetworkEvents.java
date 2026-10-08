package io.github.dirnei.mc.servus.foundation.network;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Game bus hooks that drive the network managers. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class NetworkEvents {

    private NetworkEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            NetworkManager.existing(level).ifPresent(NetworkManager::tick);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            NetworkManager.unload(level);
        }
    }
}
