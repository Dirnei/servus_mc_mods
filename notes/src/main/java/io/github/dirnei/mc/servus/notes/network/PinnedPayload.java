package io.github.dirnei.mc.servus.notes.network;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the list the player has pinned to their HUD, sent on joining and on every change. */
public record PinnedPayload(Optional<UUID> listId) implements CustomPacketPayload {

    public static final Type<PinnedPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "pinned"));

    public static final StreamCodec<FriendlyByteBuf, PinnedPayload> STREAM_CODEC =
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).map(PinnedPayload::new, PinnedPayload::listId).cast();

    @Override
    public Type<PinnedPayload> type() {
        return TYPE;
    }
}
