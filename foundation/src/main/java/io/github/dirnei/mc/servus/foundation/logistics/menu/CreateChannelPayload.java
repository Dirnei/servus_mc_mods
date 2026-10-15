package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: creates a channel for the player's team and puts one line
 * of the open endpoint GUI's side on it. An empty icon means none.
 */
public record CreateChannelPayload(int line, String name, int colour, String icon) implements CustomPacketPayload {

    public static final Type<CreateChannelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "create_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CreateChannelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CreateChannelPayload::line,
            ByteBufCodecs.STRING_UTF8, CreateChannelPayload::name,
            ByteBufCodecs.VAR_INT, CreateChannelPayload::colour,
            ByteBufCodecs.STRING_UTF8, CreateChannelPayload::icon,
            CreateChannelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
