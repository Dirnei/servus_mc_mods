package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the stock of the warehouse behind the open terminal, its
 * open requests and the last refusal message.
 *
 * @param terminal the terminal the content belongs to
 * @param online   whether the warehouse is online
 * @param reason   offline reason key, empty when online
 * @param entries  the stock, largest first
 * @param requests the terminal's open requests
 * @param message  refusal key of the last request, empty when it went through
 * @param rules    the warehouse's stock rules with their state
 * @param craftables one of every item a producer of the line can make
 */
public record StockSyncPayload(BlockPos terminal, boolean online, String reason, List<StockEntry> entries, List<RequestRow> requests,
                               String message, List<RuleRow> rules, List<ItemStack> craftables) implements CustomPacketPayload {

    public static final Type<StockSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "stock_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<StockEntry>> ENTRIES = StockEntry.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<RegistryFriendlyByteBuf, List<RequestRow>> REQUESTS = RequestRow.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<RegistryFriendlyByteBuf, List<RuleRow>> RULES = RuleRow.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> STACKS = ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list());

    /** Eight fields, more than {@code StreamCodec.composite} takes. */
    public static final StreamCodec<RegistryFriendlyByteBuf, StockSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.terminal());
                ByteBufCodecs.BOOL.encode(buf, payload.online());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.reason());
                ENTRIES.encode(buf, payload.entries());
                REQUESTS.encode(buf, payload.requests());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.message());
                RULES.encode(buf, payload.rules());
                STACKS.encode(buf, payload.craftables());
            },
            buf -> new StockSyncPayload(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ENTRIES.decode(buf),
                    REQUESTS.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), RULES.decode(buf), STACKS.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
