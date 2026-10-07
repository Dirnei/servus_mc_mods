package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the stock of the warehouse behind the open terminal, its
 * open requests and the last refusal message.
 *
 * @param online   whether the warehouse is online
 * @param reason   offline reason key, empty when online
 * @param entries  the stock, largest first
 * @param requests the terminal's open requests
 * @param message  refusal key of the last request, empty when it went through
 * @param rules    the warehouse's stock rules with their state
 */
public record StockSyncPayload(boolean online, String reason, List<StockEntry> entries, List<RequestRow> requests,
                               String message, List<RuleRow> rules) implements CustomPacketPayload {

    public static final Type<StockSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "stock_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StockSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StockSyncPayload::online,
            ByteBufCodecs.STRING_UTF8, StockSyncPayload::reason,
            StockEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), StockSyncPayload::entries,
            RequestRow.STREAM_CODEC.apply(ByteBufCodecs.list()), StockSyncPayload::requests,
            ByteBufCodecs.STRING_UTF8, StockSyncPayload::message,
            RuleRow.STREAM_CODEC.apply(ByteBufCodecs.list()), StockSyncPayload::rules,
            StockSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
