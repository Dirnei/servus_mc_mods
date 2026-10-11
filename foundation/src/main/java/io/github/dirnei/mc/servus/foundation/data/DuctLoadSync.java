package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.data.packet.DuctBudget;
import io.github.dirnei.mc.servus.foundation.item.GogglesItem;
import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sends the load levels of busy data ducts near each player who reveals
 * hidden things (goggles worn or Configuration Carrot held), twice a second
 * and only when they changed, so the overlay never queries per frame.
 */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class DuctLoadSync {

    private static final int INTERVAL = 10;
    private static final double RANGE = 48;
    private static final Map<UUID, Map<Long, Integer>> SENT = new HashMap<>();

    private DuctLoadSync() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % INTERVAL != 0) {
            return;
        }
        Map<Long, Integer> levels = GogglesItem.reveals(player) ? levels(player) : Map.of();
        Map<Long, Integer> last = SENT.getOrDefault(player.getUUID(), Map.of());
        if (!levels.equals(last)) {
            SENT.put(player.getUUID(), levels);
            PacketDistributor.sendToPlayer(player, new DuctLoadPayload(levels));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    /** The load levels of the busy data ducts within range of the player. */
    static Map<Long, Integer> levels(ServerPlayer player) {
        Map<Long, Integer> levels = new TreeMap<>();
        ServerLevel level = player.level();
        double rangeSq = RANGE * RANGE;
        for (DataNetwork network : NetworkManager.of(level).set(DataNetwork.TYPE).networks()) {
            for (DataNetwork.DuctLoad load : network.loads()) {
                if (load.pos().getCenter().distanceToSqr(player.position()) <= rangeSq) {
                    levels.put(load.pos().asLong(), DuctBudget.level(load.load(), load.bandwidth()));
                }
            }
        }
        return levels;
    }
}
