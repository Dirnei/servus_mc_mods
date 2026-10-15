package io.github.dirnei.mc.servus.foundation.logistics.channel;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends the channel list to a player on login and to every player after a change. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class ChannelSync {

    private ChannelSync() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, ChannelListPayload.of(DuctChannelData.get(player.level().getServer()).list()));
        }
    }

    static void broadcast(ChannelList list) {
        PacketDistributor.sendToAllPlayers(ChannelListPayload.of(list));
    }
}
