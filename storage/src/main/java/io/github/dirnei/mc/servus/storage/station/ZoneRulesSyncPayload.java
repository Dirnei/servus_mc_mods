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
 * Server to client: the zones of the warehouse behind the open station and the station's state.
 *
 * @param online        whether the warehouse is online
 * @param reason        offline reason key, empty when online
 * @param zones         every zone with its block count and the filters its blocks carry
 * @param blockedReason the station's blocked reason name, empty when nothing waits
 * @param blockedStack  the first waiting item, empty when nothing waits
 * @param blockedZone   the zone the waiting item matched, empty for general storage
 * @param idle          whether the buffer is empty
 */
public record ZoneRulesSyncPayload(boolean online, String reason, List<ZoneRow> zones, String blockedReason,
                                   ItemStack blockedStack, String blockedZone, boolean idle) implements CustomPacketPayload {

    /** One zone in the list. */
    public record ZoneRow(String key, String display, int blocks, List<String> items, List<String> tags) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ZoneRow> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ZoneRow::key,
                ByteBufCodecs.STRING_UTF8, ZoneRow::display,
                ByteBufCodecs.VAR_INT, ZoneRow::blocks,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ZoneRow::items,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ZoneRow::tags,
                ZoneRow::new);
    }

    public static final Type<ZoneRulesSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "zone_rules_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ZoneRulesSyncPayload> STREAM_CODEC =
            StreamCodec.of(ZoneRulesSyncPayload::write, ZoneRulesSyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, ZoneRulesSyncPayload payload) {
        buf.writeBoolean(payload.online);
        buf.writeUtf(payload.reason);
        ZoneRow.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, payload.zones);
        buf.writeUtf(payload.blockedReason);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.blockedStack);
        buf.writeUtf(payload.blockedZone);
        buf.writeBoolean(payload.idle);
    }

    private static ZoneRulesSyncPayload read(RegistryFriendlyByteBuf buf) {
        boolean online = buf.readBoolean();
        String reason = buf.readUtf();
        List<ZoneRow> zones = ZoneRow.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
        String blockedReason = buf.readUtf();
        ItemStack blockedStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        String blockedZone = buf.readUtf();
        boolean idle = buf.readBoolean();
        return new ZoneRulesSyncPayload(online, reason, zones, blockedReason, blockedStack, blockedZone, idle);
    }

    public static final ZoneRulesSyncPayload EMPTY = new ZoneRulesSyncPayload(false, "", List.of(), "", ItemStack.EMPTY, "", true);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
