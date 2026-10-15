package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: changes name, colour and icon of one of the team's channels. An empty icon means none. */
public record EditChannelPayload(int id, String name, int colour, String icon) implements CustomPacketPayload {

    public static final Type<EditChannelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "edit_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EditChannelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EditChannelPayload::id,
            ByteBufCodecs.STRING_UTF8, EditChannelPayload::name,
            ByteBufCodecs.VAR_INT, EditChannelPayload::colour,
            ByteBufCodecs.STRING_UTF8, EditChannelPayload::icon,
            EditChannelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
