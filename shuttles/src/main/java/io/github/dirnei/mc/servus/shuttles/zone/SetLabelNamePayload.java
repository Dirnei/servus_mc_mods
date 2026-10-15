package io.github.dirnei.mc.servus.shuttles.zone;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: name the Zone Label open in the label menu. */
public record SetLabelNamePayload(String name) implements CustomPacketPayload {

    public static final Type<SetLabelNamePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "set_label_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetLabelNamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetLabelNamePayload::name,
            SetLabelNamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
