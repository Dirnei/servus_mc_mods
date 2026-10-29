package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: step the handheld in the main hand {@code step} modes on, negative to go back. */
public record HandheldModePayload(int step) implements CustomPacketPayload {

    public static final Type<HandheldModePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_mode"));

    public static final StreamCodec<ByteBuf, HandheldModePayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(HandheldModePayload::new, HandheldModePayload::step);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
