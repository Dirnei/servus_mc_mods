package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: the stock of the warehouse behind the open terminal. */
public record StockSyncPayload(boolean online, String reason, List<StockEntry> entries) implements CustomPacketPayload {

    public static final Type<StockSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "stock_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StockSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StockSyncPayload::online,
            ByteBufCodecs.STRING_UTF8, StockSyncPayload::reason,
            StockEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), StockSyncPayload::entries,
            StockSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
