package io.github.dirnei.mc.servus.notes.network;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the list was deleted or the player no longer sees it. The client ignores ids it does not have. */
public record ListGonePayload(UUID listId) implements CustomPacketPayload {

    public static final Type<ListGonePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "list_gone"));

    public static final StreamCodec<FriendlyByteBuf, ListGonePayload> STREAM_CODEC =
            UUIDUtil.STREAM_CODEC.map(ListGonePayload::new, ListGonePayload::listId).cast();

    @Override
    public Type<ListGonePayload> type() {
        return TYPE;
    }
}
