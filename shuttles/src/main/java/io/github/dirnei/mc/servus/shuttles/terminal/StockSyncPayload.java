package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

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
 * @param sites    the other sites the terminal can look at, empty while this site's controller is not wireless capable
 * @param view     the key of the site shown, empty for this site
 * @param viewState the {@code RemoteSite.State} ordinal of the site shown, -1 for this site
 * @param noPower  whether a warehouse the terminal reaches is in the no power state
 * @param orderGrid whether a formed Crafting CPU and a Crafting Station are on the terminal's data line
 * @param materials the craftables that stand for a material pattern, with what their material follows
 */
public record StockSyncPayload(BlockPos terminal, boolean online, String reason, List<StockEntry> entries, List<RequestRow> requests,
                               String message, List<RuleRow> rules, List<ItemStack> craftables, List<SiteRow> sites, String view,
                               int viewState, boolean noPower, boolean orderGrid, List<MaterialCraftable> materials)
        implements CustomPacketPayload {

    /** The content of this site only. */
    public StockSyncPayload(BlockPos terminal, boolean online, String reason, List<StockEntry> entries, List<RequestRow> requests,
                            String message, List<RuleRow> rules, List<ItemStack> craftables) {
        this(terminal, online, reason, entries, requests, message, rules, craftables, List.of(), "", -1, false, false, List.of());
    }

    public static final Type<StockSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "stock_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<StockEntry>> ENTRIES = StockEntry.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<RegistryFriendlyByteBuf, List<RequestRow>> REQUESTS = RequestRow.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<RegistryFriendlyByteBuf, List<RuleRow>> RULES = RuleRow.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> STACKS = ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<RegistryFriendlyByteBuf, List<MaterialCraftable>> MATERIALS =
            MaterialCraftable.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<io.netty.buffer.ByteBuf, List<SiteRow>> SITES = SiteRow.STREAM_CODEC.apply(ByteBufCodecs.list());

    /** Fourteen fields, more than {@code StreamCodec.composite} takes. */
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
                SITES.encode(buf, payload.sites());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.view());
                ByteBufCodecs.VAR_INT.encode(buf, payload.viewState());
                ByteBufCodecs.BOOL.encode(buf, payload.noPower());
                ByteBufCodecs.BOOL.encode(buf, payload.orderGrid());
                MATERIALS.encode(buf, payload.materials());
            },
            buf -> new StockSyncPayload(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ENTRIES.decode(buf),
                    REQUESTS.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), RULES.decode(buf), STACKS.decode(buf), SITES.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf), MATERIALS.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
