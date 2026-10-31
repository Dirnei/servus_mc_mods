package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server, SEND in the locker picker: order the STOCK basket to the Parcel Locker whose dock is at {@code locker}. */
public record HandheldRequestPayload(BlockPos locker) implements CustomPacketPayload {

    public static final Type<HandheldRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_request"));

    public static final StreamCodec<ByteBuf, HandheldRequestPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(HandheldRequestPayload::new, HandheldRequestPayload::locker);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
