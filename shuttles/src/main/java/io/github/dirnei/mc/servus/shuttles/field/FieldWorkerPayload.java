package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: the name plates of the harvester and of the Spreader that
 * work the field of the open Field Head, empty for none or no plate.
 */
public record FieldWorkerPayload(String name, String spreader) implements CustomPacketPayload {

    public static final Type<FieldWorkerPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "field_worker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FieldWorkerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), FieldWorkerPayload::name,
            ByteBufCodecs.stringUtf8(64), FieldWorkerPayload::spreader,
            FieldWorkerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
