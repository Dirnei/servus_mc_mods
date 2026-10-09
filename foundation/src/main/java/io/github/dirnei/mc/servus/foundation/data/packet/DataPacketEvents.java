package io.github.dirnei.mc.servus.foundation.data.packet;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Game bus hooks for data packets: limits from data, and stashed items dropping once their place loads. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class DataPacketEvents {

    private DataPacketEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(PacketLimitsManager.INSTANCE);
        event.addListener(io.github.dirnei.mc.servus.foundation.data.CableTiersManager.INSTANCE);
        event.addListener(io.github.dirnei.mc.servus.foundation.upgrade.UpgradesManager.INSTANCE);
    }

    /** Packets are not saved: settle them while every chunk is still loaded, before the worlds save. */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DataPackets.settleAll(event.getServer());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PacketStash stash = PacketStash.of(level);
            if (!stash.isEmpty()) {
                stash.tick(level);
            }
        }
    }
}
