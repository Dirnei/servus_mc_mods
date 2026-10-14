package io.github.dirnei.mc.servus.manufacturing.journal;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Game bus hooks of the journal: chapter data and catching up on login. */
@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID)
public final class JournalEvents {

    private JournalEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(JournalChapters.LISTENER_ID, JournalChapters.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        JournalCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Journal.catchUp(player);
            JournalBookItem.giveOnce(player);
        }
    }
}
