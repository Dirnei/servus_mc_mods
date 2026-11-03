package io.github.dirnei.mc.servus.notes.network;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.ListView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: one list the player sees, new or changed, with the player's rights on it. */
public record ListPayload(ListView view) implements CustomPacketPayload {

    public static final Type<ListPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "list"));

    public static final StreamCodec<FriendlyByteBuf, ListPayload> STREAM_CODEC =
            NotesStreamCodecs.VIEW.map(ListPayload::new, ListPayload::view);

    @Override
    public Type<ListPayload> type() {
        return TYPE;
    }
}
