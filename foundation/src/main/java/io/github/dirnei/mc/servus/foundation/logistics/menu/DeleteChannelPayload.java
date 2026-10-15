package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: deletes one of the team's channels from the endpoint GUI of a line's side. */
public record DeleteChannelPayload(int line, int id) implements CustomPacketPayload {

    public static final Type<DeleteChannelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "delete_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteChannelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeleteChannelPayload::line,
            ByteBufCodecs.VAR_INT, DeleteChannelPayload::id,
            DeleteChannelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
