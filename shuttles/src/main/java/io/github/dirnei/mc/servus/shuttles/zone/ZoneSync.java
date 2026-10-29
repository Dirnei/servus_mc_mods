package io.github.dirnei.mc.servus.shuttles.zone;

import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseEvent;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends the zone list to a player on login and to every player after a change, and tells the warehouses. */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID)
public final class ZoneSync {

    private ZoneSync() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, ZoneListPayload.of(WarehouseZoneData.get(player.level().getServer()).list()));
        }
    }

    static void broadcast(ZoneList<ZoneFilter> list) {
        PacketDistributor.sendToAllPlayers(ZoneListPayload.of(list));
    }

    /** Posts {@link WarehouseEvent.ZonesEdited} to every warehouse of every loaded dimension. */
    static void tellWarehouses(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (WarehouseNetwork network : NetworkManager.of(level).set(WarehouseNetwork.TYPE).networks()) {
                network.post(new WarehouseEvent.ZonesEdited());
            }
        }
    }
}
