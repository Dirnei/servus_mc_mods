package io.github.dirnei.mc.servus.alchemy.hollow;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Sends every player the loaded veins at login and after {@code /reload}. */
@EventBusSubscriber(modid = ServusAlchemyIds.MOD_ID)
public final class VeinSync {

    private VeinSync() {
    }

    public static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, VeinSyncPayload.of(VeinData.INSTANCE.veins()));
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(VeinSyncPayload.TYPE, VeinSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientVeins.update(payload));
    }

    /** Fires for a joining player and for everyone after a reload. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(VeinSync::send);
    }
}
