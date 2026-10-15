package io.github.dirnei.mc.servus.alchemy.journal;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Sends each player their team's journal progress: at login, after {@code /reload}, and once per server tick for
 * teams whose progress changed.
 */
@EventBusSubscriber(modid = ServusAlchemyIds.MOD_ID)
public final class JournalSync {

    private static final Set<UUID> CHANGED = new HashSet<>();

    private JournalSync() {
    }

    /** Queues the team's progress to be sent at the end of this tick. Server thread only. */
    public static void markChanged(UUID team) {
        CHANGED.add(team);
    }

    /** Whether the team waits to be sent; for tests. */
    public static boolean isQueued(UUID team) {
        return CHANGED.contains(team);
    }

    /** Sends every queued team's progress to its online members and empties the queue. */
    public static void drain(MinecraftServer server) {
        if (CHANGED.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (CHANGED.contains(Teams.teamOf(player))) {
                send(player);
            }
        }
        CHANGED.clear();
    }

    public static void send(ServerPlayer player) {
        JournalLedger.TeamJournal journal = Journal.journalOf(player.level().getServer(), Teams.teamOf(player));
        PacketDistributor.sendToPlayer(player,
                new JournalProgressPayload(JournalProgress.snapshot(JournalChapters.INSTANCE.chapters(), journal)));
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(JournalProgressPayload.TYPE, JournalProgressPayload.STREAM_CODEC,
                (payload, context) -> ClientJournalProgress.update(payload));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        drain(event.getServer());
    }

    /** Fires for a joining player and for everyone after a reload. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(JournalSync::send);
    }
}
