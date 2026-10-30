package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: switch the handheld's mode, to {@code target} when given
 * (a softkey of the open GUI), else {@code step} modes on, negative to go back
 * (sneak and scroll with the handheld in the main hand).
 */
public record HandheldModePayload(int step, Optional<HandheldMode> target) implements CustomPacketPayload {

    public static final Type<HandheldModePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_mode"));

    public static final StreamCodec<ByteBuf, HandheldModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HandheldModePayload::step,
            ByteBufCodecs.optional(HandheldMode.STREAM_CODEC), HandheldModePayload::target,
            HandheldModePayload::new);

    /** Steps the mode, as sneak and scroll does. */
    public static HandheldModePayload step(int step) {
        return new HandheldModePayload(step, Optional.empty());
    }

    /** Switches straight to a mode, as a softkey does. */
    public static HandheldModePayload to(HandheldMode mode) {
        return new HandheldModePayload(0, Optional.of(mode));
    }

    /** The mode a handheld in {@code current} switches to. */
    public HandheldMode next(HandheldMode current) {
        return target.orElseGet(() -> current.step(step));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
