package io.github.dirnei.mc.servus.notes.network;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.Limits;
import io.github.dirnei.mc.servus.notes.model.ListView;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: every list the player sees, replacing what the client had, plus the server's limits. */
public record ListsPayload(List<ListView> lists, Limits limits) implements CustomPacketPayload {

    public static final Type<ListsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "lists"));

    public static final StreamCodec<FriendlyByteBuf, ListsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                NotesStreamCodecs.writeList(buf, payload.lists(), NotesStreamCodecs.VIEW);
                NotesStreamCodecs.LIMITS.encode(buf, payload.limits());
            },
            buf -> new ListsPayload(NotesStreamCodecs.readList(buf, NotesStreamCodecs.VIEW), NotesStreamCodecs.LIMITS.decode(buf)));

    public ListsPayload {
        lists = List.copyOf(lists);
    }

    @Override
    public Type<ListsPayload> type() {
        return TYPE;
    }
}
