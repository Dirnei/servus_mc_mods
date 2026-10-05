package io.github.dirnei.mc.servus.logistics.network;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: take one channel out of a duct and give it to the player. */
public record RemoveChannelPayload(BlockPos pos, int channel) implements CustomPacketPayload {

    public static final Type<RemoveChannelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusLogisticsIds.MOD_ID, "remove_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveChannelPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RemoveChannelPayload::pos,
            ByteBufCodecs.VAR_INT, RemoveChannelPayload::channel,
            RemoveChannelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
