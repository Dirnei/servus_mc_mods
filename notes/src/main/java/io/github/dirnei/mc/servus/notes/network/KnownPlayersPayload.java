package io.github.dirnei.mc.servus.notes.network;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.Person;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: every player who has been on the server, online or not, to pick guests from. */
public record KnownPlayersPayload(List<Person> players) implements CustomPacketPayload {

    public static final Type<KnownPlayersPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "known_players"));

    public static final StreamCodec<FriendlyByteBuf, KnownPlayersPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> NotesStreamCodecs.writeList(buf, payload.players(), NotesStreamCodecs.PERSON),
            buf -> new KnownPlayersPayload(NotesStreamCodecs.readList(buf, NotesStreamCodecs.PERSON)));

    public KnownPlayersPayload {
        players = List.copyOf(players);
    }

    @Override
    public Type<KnownPlayersPayload> type() {
        return TYPE;
    }
}
