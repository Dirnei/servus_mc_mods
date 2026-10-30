package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: the handheld scanned the shelf of these bays. The client flashes them
 * green or red and beeps or buzzes.
 *
 * @param bays       every bay of the scanned shelf
 * @param ok         green with a beep, or red with a buzz
 * @param sweepIndex the shelf's place in a held sweep, 0 for the first; the beep rises with it
 */
public record HandheldScanPayload(List<BlockPos> bays, boolean ok, int sweepIndex) implements CustomPacketPayload {

    public static final Type<HandheldScanPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_scan"));

    public static final StreamCodec<ByteBuf, HandheldScanPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), HandheldScanPayload::bays,
            ByteBufCodecs.BOOL, HandheldScanPayload::ok,
            ByteBufCodecs.VAR_INT, HandheldScanPayload::sweepIndex,
            HandheldScanPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
