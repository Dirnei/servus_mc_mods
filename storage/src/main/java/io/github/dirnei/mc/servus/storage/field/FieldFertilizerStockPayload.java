package io.github.dirnei.mc.servus.storage.field;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the warehouse stock of the fertilizer items for the open
 * Field Head menu with that container id. Only non zero counts travel;
 * without a warehouse {@code networked} is false and the picker shows no counts.
 */
public record FieldFertilizerStockPayload(int containerId, boolean networked, Map<ResourceLocation, Long> counts)
        implements CustomPacketPayload {

    public static final Type<FieldFertilizerStockPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "field_fertilizer_stock"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FieldFertilizerStockPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FieldFertilizerStockPayload::containerId,
            ByteBufCodecs.BOOL, FieldFertilizerStockPayload::networked,
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_LONG), FieldFertilizerStockPayload::counts,
            FieldFertilizerStockPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
