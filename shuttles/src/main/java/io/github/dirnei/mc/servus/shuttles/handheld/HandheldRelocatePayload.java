package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server, the Relocate button of the INFO screen: close the GUI and wait for the target shelf. */
public record HandheldRelocatePayload() implements CustomPacketPayload {

    public static final HandheldRelocatePayload INSTANCE = new HandheldRelocatePayload();

    public static final Type<HandheldRelocatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_relocate"));

    public static final StreamCodec<ByteBuf, HandheldRelocatePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
