package io.github.dirnei.mc.servus.notes.network;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the player's last edit was refused, with the {@link io.github.dirnei.mc.servus.notes.model.Refusal#key() reason key}. */
public record RefusedPayload(String reasonKey) implements CustomPacketPayload {

    public static final Type<RefusedPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "refused"));

    public static final StreamCodec<FriendlyByteBuf, RefusedPayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(64).map(RefusedPayload::new, RefusedPayload::reasonKey).cast();

    @Override
    public Type<RefusedPayload> type() {
        return TYPE;
    }
}
