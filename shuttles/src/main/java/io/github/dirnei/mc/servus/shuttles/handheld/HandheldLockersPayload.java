package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client, the answer to REQUEST: the Parcel Lockers of the bound
 * warehouse for the locker picker, or why none can be chosen. The client
 * sorts them by its own distance to each.
 *
 * @param problem the lang key suffix of why nothing can be sent, empty when lockers are listed
 * @param lockers every locker of the warehouse
 */
public record HandheldLockersPayload(String problem, List<Row> lockers) implements CustomPacketPayload {

    /**
     * One locker of the picker.
     *
     * @param label  its label, such as "Parcel Locker #1"
     * @param dock   where its dock stands
     * @param free   its free boxes per size, S, M and L
     * @param fits   whether its free boxes take the whole order
     */
    public record Row(Component label, BlockPos dock, List<Integer> free, boolean fits) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
                ComponentSerialization.STREAM_CODEC, Row::label,
                BlockPos.STREAM_CODEC, Row::dock,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), Row::free,
                ByteBufCodecs.BOOL, Row::fits,
                Row::new);
    }

    public static final Type<HandheldLockersPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_lockers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandheldLockersPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, HandheldLockersPayload::problem,
            Row.STREAM_CODEC.apply(ByteBufCodecs.list()), HandheldLockersPayload::lockers,
            HandheldLockersPayload::new);

    public static HandheldLockersPayload problem(String problem) {
        return new HandheldLockersPayload(problem, List.of());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
