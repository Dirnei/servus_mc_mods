package io.github.dirnei.mc.servus.notes.network;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: one edit the player asks for; the server checks it before applying. */
public record EditPayload(NoteEdit edit) implements CustomPacketPayload {

    public static final Type<EditPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "edit"));

    public static final StreamCodec<FriendlyByteBuf, EditPayload> STREAM_CODEC =
            NotesStreamCodecs.EDIT.map(EditPayload::new, EditPayload::edit);

    @Override
    public Type<EditPayload> type() {
        return TYPE;
    }
}
