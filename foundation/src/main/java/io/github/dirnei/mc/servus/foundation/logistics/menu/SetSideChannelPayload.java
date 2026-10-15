package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: puts one line of the open endpoint GUI's side on a channel id; 0 is Default. */
public record SetSideChannelPayload(int line, int channel) implements CustomPacketPayload {

    public static final Type<SetSideChannelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "set_side_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSideChannelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetSideChannelPayload::line,
            ByteBufCodecs.VAR_INT, SetSideChannelPayload::channel,
            SetSideChannelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
