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
 * Server to client: the warehouse stock of the crop items for the open Field
 * Head menu with that container id. Only non zero counts travel; without a
 * warehouse {@code networked} is false and the picker shows no counts.
 */
public record FieldCropStockPayload(int containerId, boolean networked, Map<ResourceLocation, Long> counts)
        implements CustomPacketPayload {

    public static final Type<FieldCropStockPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "field_crop_stock"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FieldCropStockPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FieldCropStockPayload::containerId,
            ByteBufCodecs.BOOL, FieldCropStockPayload::networked,
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_LONG), FieldCropStockPayload::counts,
            FieldCropStockPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
