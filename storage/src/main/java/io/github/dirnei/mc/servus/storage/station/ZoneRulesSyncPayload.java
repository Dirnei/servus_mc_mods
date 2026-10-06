package io.github.dirnei.mc.servus.storage.station;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: the zones of the warehouse behind the open station, the
 * selected zone's rule and the station's state.
 *
 * @param online       whether the warehouse is online
 * @param reason       offline reason key, empty when online
 * @param zones        every zone with its block count and flags
 * @param selected     the selected zone key, empty for none
 * @param tagLine      the selected zone's tags, comma separated
 * @param overflow     whether the selected zone is the overflow zone
 * @param blockedReason the station's blocked reason name, empty when nothing waits
 * @param blockedStack the first waiting item, empty when nothing waits
 * @param blockedZone  the zone the waiting item belongs to, empty for general storage
 * @param idle         whether the buffer is empty
 */
public record ZoneRulesSyncPayload(boolean online, String reason, List<ZoneRow> zones, String selected, String tagLine,
                                   boolean overflow, String blockedReason, ItemStack blockedStack, String blockedZone,
                                   boolean idle) implements CustomPacketPayload {

    /** One zone in the list. */
    public record ZoneRow(String key, String display, int blocks, boolean filtered, boolean overflow) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ZoneRow> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ZoneRow::key,
                ByteBufCodecs.STRING_UTF8, ZoneRow::display,
                ByteBufCodecs.VAR_INT, ZoneRow::blocks,
                ByteBufCodecs.BOOL, ZoneRow::filtered,
                ByteBufCodecs.BOOL, ZoneRow::overflow,
                ZoneRow::new);
    }

    public static final Type<ZoneRulesSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "zone_rules_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ZoneRulesSyncPayload> STREAM_CODEC = StreamCodec.of(
            ZoneRulesSyncPayload::write, ZoneRulesSyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, ZoneRulesSyncPayload payload) {
        buf.writeBoolean(payload.online);
        buf.writeUtf(payload.reason);
        ZoneRow.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, payload.zones);
        buf.writeUtf(payload.selected);
        buf.writeUtf(payload.tagLine);
        buf.writeBoolean(payload.overflow);
        buf.writeUtf(payload.blockedReason);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.blockedStack);
        buf.writeUtf(payload.blockedZone);
        buf.writeBoolean(payload.idle);
    }

    private static ZoneRulesSyncPayload read(RegistryFriendlyByteBuf buf) {
        boolean online = buf.readBoolean();
        String reason = buf.readUtf();
        List<ZoneRow> zones = ZoneRow.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
        String selected = buf.readUtf();
        String tagLine = buf.readUtf();
        boolean overflow = buf.readBoolean();
        String blockedReason = buf.readUtf();
        ItemStack blockedStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        String blockedZone = buf.readUtf();
        boolean idle = buf.readBoolean();
        return new ZoneRulesSyncPayload(online, reason, zones, selected, tagLine, overflow, blockedReason, blockedStack, blockedZone, idle);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
