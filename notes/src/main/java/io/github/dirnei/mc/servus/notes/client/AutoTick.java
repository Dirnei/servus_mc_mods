package io.github.dirnei.mc.servus.notes.client;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.AutoTicks;
import io.github.dirnei.mc.servus.notes.model.ListView;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Ticks the item tasks this player claims once their inventory holds every missing item. A task is ticked
 * this way at most once per session, so unticking it by hand sticks.
 */
@EventBusSubscriber(modid = ServusNotesIds.MOD_ID, value = Dist.CLIENT)
public final class AutoTick {

    private static final int INTERVAL_TICKS = 20;

    private static final Set<UUID> ticked = new HashSet<>();
    private static int countdown;

    private AutoTick() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (--countdown > 0) {
            return;
        }
        countdown = INTERVAL_TICKS;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        UUID self = ClientNotes.self();
        for (ListView view : ClientNotes.lists()) {
            if (!view.rights().tick()) {
                continue;
            }
            for (UUID taskId : AutoTicks.due(view.list(), self, itemId -> Carried.count(player, itemId))) {
                if (ticked.add(taskId)) {
                    ClientNotes.send(new NoteEdit.Tick(view.list().id(), taskId, true));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ticked.clear();
    }
}
