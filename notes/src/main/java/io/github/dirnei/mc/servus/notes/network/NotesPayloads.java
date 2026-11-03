package io.github.dirnei.mc.servus.notes.network;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.client.ClientNotes;
import io.github.dirnei.mc.servus.notes.server.NotesService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers the Notes payloads and sends each player their lists and the known players when they join. */
@EventBusSubscriber(modid = ServusNotesIds.MOD_ID)
public final class NotesPayloads {

    private NotesPayloads() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ListsPayload.TYPE, ListsPayload.STREAM_CODEC,
                (payload, context) -> ClientNotes.replaceAll(payload.lists(), payload.limits()));
        registrar.playToClient(ListPayload.TYPE, ListPayload.STREAM_CODEC,
                (payload, context) -> ClientNotes.update(payload.view()));
        registrar.playToClient(ListGonePayload.TYPE, ListGonePayload.STREAM_CODEC,
                (payload, context) -> ClientNotes.remove(payload.listId()));
        registrar.playToClient(RefusedPayload.TYPE, RefusedPayload.STREAM_CODEC,
                (payload, context) -> ClientNotes.refused(payload.reasonKey()));
        registrar.playToClient(ViewerPayload.TYPE, ViewerPayload.STREAM_CODEC,
                (payload, context) -> ClientNotes.viewer(payload.teamsInstalled(), payload.party()));
        registrar.playToClient(KnownPlayersPayload.TYPE, KnownPlayersPayload.STREAM_CODEC,
                (payload, context) -> ClientNotes.knownPlayers(payload.players()));
        registrar.playToClient(PinnedPayload.TYPE, PinnedPayload.STREAM_CODEC,
                (payload, context) -> ClientNotes.pinned(payload.listId()));
        registrar.playToServer(PinPayload.TYPE, PinPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                NotesService.pin(player.level().getServer(), player.getUUID(), payload.listId());
            }
        });
        registrar.playToServer(EditPayload.TYPE, EditPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                NotesService.apply(player, payload.edit());
            }
        });
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NotesService.joined(player);
        }
    }
}
