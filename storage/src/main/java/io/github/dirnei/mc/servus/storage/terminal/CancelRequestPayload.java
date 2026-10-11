package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: cancel an open request at the open terminal. */
public record CancelRequestPayload(int id) implements CustomPacketPayload {

    public static final Type<CancelRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "cancel_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CancelRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CancelRequestPayload::id,
            CancelRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
