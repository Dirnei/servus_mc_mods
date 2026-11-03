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

/** Client to server: pin this list to the player's HUD, or clear the pin when empty. Refused for a list the player does not see. */
public record PinPayload(Optional<UUID> listId) implements CustomPacketPayload {

    public static final Type<PinPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "pin"));

    public static final StreamCodec<FriendlyByteBuf, PinPayload> STREAM_CODEC =
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).map(PinPayload::new, PinPayload::listId).cast();

    @Override
    public Type<PinPayload> type() {
        return TYPE;
    }
}
