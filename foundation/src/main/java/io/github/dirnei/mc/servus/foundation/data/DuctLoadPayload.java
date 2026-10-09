package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the load levels of the busy data ducts near a player who
 * reveals hidden things, keyed by packed block position. Ducts not listed
 * are idle. Sent only when the levels change.
 *
 * @param levels load level per duct, 1 to {@code DuctBudget.FULL}
 */
public record DuctLoadPayload(Map<Long, Integer> levels) implements CustomPacketPayload {

    public static final Type<DuctLoadPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "duct_load"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DuctLoadPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.levels().size());
                payload.levels().forEach((pos, level) -> {
                    buf.writeLong(pos);
                    buf.writeByte(level);
                });
            },
            buf -> {
                int size = buf.readVarInt();
                Map<Long, Integer> levels = new LinkedHashMap<>();
                for (int i = 0; i < size; i++) {
                    levels.put(buf.readLong(), (int) buf.readByte());
                }
                return new DuctLoadPayload(levels);
            });

    public static final DuctLoadPayload EMPTY = new DuctLoadPayload(Map.of());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
