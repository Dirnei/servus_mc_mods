package io.github.dirnei.mc.servus.logistics.network;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: set the mode of one channel on one side of a duct. */
public record SetDuctModePayload(BlockPos pos, int channel, int side, int mode) implements CustomPacketPayload {

    public static final Type<SetDuctModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusLogisticsIds.MOD_ID, "set_duct_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetDuctModePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetDuctModePayload::pos,
            ByteBufCodecs.VAR_INT, SetDuctModePayload::channel,
            ByteBufCodecs.VAR_INT, SetDuctModePayload::side,
            ByteBufCodecs.VAR_INT, SetDuctModePayload::mode,
            SetDuctModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
