package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server, REQUEST on the STOCK screen: send the lockers the basket could go to. */
public record HandheldPickerPayload() implements CustomPacketPayload {

    public static final HandheldPickerPayload INSTANCE = new HandheldPickerPayload();

    public static final Type<HandheldPickerPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_picker"));

    public static final StreamCodec<ByteBuf, HandheldPickerPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
