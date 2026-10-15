package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: the warehouse stock of the fertilizer and anti-corrosion items for the open
 * Field Head menu with that container id. Only non zero counts travel;
 * without a warehouse {@code networked} is false and the picker shows no counts.
 */
public record FieldSupplyStockPayload(int containerId, boolean networked, Map<Identifier, Long> counts)
        implements CustomPacketPayload {

    public static final Type<FieldSupplyStockPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "field_supply_stock"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FieldSupplyStockPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FieldSupplyStockPayload::containerId,
            ByteBufCodecs.BOOL, FieldSupplyStockPayload::networked,
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_LONG), FieldSupplyStockPayload::counts,
            FieldSupplyStockPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
