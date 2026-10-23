package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: what crafting {@code amount} of {@code item} would do.
 *
 * @param fromStock    stock the order would reserve
 * @param toCraft      what its steps would make
 * @param missing      raw material it cannot get
 * @param refusal      {@code no_cpu}, {@code no_producer}, {@code no_station}, {@code no_match} or {@code offline}, else empty
 * @param inStock      for a request: how many of the item are shipped from stock, the rest is crafted
 * @param patternSaved for the order grid: whether a pattern of the line has its recipe and items
 */
public record CraftPreviewPayload(ItemStack item, int amount, List<StockEntry> fromStock, List<StockEntry> toCraft,
                                  List<StockEntry> missing, String refusal, int inStock, boolean patternSaved)
        implements CustomPacketPayload {

    /** A preview of crafting only. */
    public CraftPreviewPayload(ItemStack item, int amount, List<StockEntry> fromStock, List<StockEntry> toCraft,
                               List<StockEntry> missing, String refusal) {
        this(item, amount, fromStock, toCraft, missing, refusal, 0, false);
    }

    public static final Type<CraftPreviewPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "craft_preview"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<StockEntry>> ROWS = StockEntry.STREAM_CODEC.apply(ByteBufCodecs.list());

    /** Eight fields, written out by hand. */
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPreviewPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.item());
                ByteBufCodecs.VAR_INT.encode(buf, payload.amount());
                ROWS.encode(buf, payload.fromStock());
                ROWS.encode(buf, payload.toCraft());
                ROWS.encode(buf, payload.missing());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.refusal());
                ByteBufCodecs.VAR_INT.encode(buf, payload.inStock());
                ByteBufCodecs.BOOL.encode(buf, payload.patternSaved());
            },
            buf -> new CraftPreviewPayload(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ROWS.decode(buf), ROWS.decode(buf), ROWS.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.BOOL.decode(buf)));

    /** Whether Start may be pressed for this preview. */
    public boolean startable() {
        return refusal.isEmpty() && missing.isEmpty() && !toCraft.isEmpty();
    }

    /** Whether a request may be confirmed: all from stock, or the crafted rest is startable. */
    public boolean requestable() {
        return inStock >= amount || startable();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
