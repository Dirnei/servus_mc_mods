package io.github.dirnei.mc.servus.storage.station;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: the state of the open inbound or outbound station.
 *
 * @param online        whether the warehouse is online
 * @param reason        offline reason key, empty when online
 * @param blockedReason the station's blocked reason name, or the outbound shortage key, empty when nothing waits
 * @param blockedStack  the first waiting item, empty when nothing waits
 * @param blockedZone   the zone the waiting item matched, empty for general storage
 * @param idle          whether the buffer is empty
 * @param sides         six side mode symbols in {@code Direction} order
 */
public record StationStatePayload(boolean online, String reason, String blockedReason, ItemStack blockedStack,
                                  String blockedZone, boolean idle, String sides) implements CustomPacketPayload {

    public static final Type<StationStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "station_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationStatePayload> STREAM_CODEC =
            StreamCodec.of(StationStatePayload::write, StationStatePayload::read);

    public static final StationStatePayload EMPTY = new StationStatePayload(false, "", "", ItemStack.EMPTY, "", true, "");

    private static void write(RegistryFriendlyByteBuf buf, StationStatePayload payload) {
        buf.writeBoolean(payload.online);
        buf.writeUtf(payload.reason);
        buf.writeUtf(payload.blockedReason);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.blockedStack);
        buf.writeUtf(payload.blockedZone);
        buf.writeBoolean(payload.idle);
        buf.writeUtf(payload.sides);
    }

    private static StationStatePayload read(RegistryFriendlyByteBuf buf) {
        boolean online = buf.readBoolean();
        String reason = buf.readUtf();
        String blockedReason = buf.readUtf();
        ItemStack blockedStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        String blockedZone = buf.readUtf();
        boolean idle = buf.readBoolean();
        String sides = buf.readUtf();
        return new StationStatePayload(online, reason, blockedReason, blockedStack, blockedZone, idle, sides);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
