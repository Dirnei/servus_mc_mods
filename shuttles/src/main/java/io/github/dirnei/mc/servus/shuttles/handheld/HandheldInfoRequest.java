package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: what does the bay at {@code pos} hold? Sent by the INFO panel when the aimed bay changes. */
public record HandheldInfoRequest(BlockPos pos) implements CustomPacketPayload {

    public static final Type<HandheldInfoRequest> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_info_request"));

    public static final StreamCodec<ByteBuf, HandheldInfoRequest> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(HandheldInfoRequest::new, HandheldInfoRequest::pos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
