package io.github.dirnei.mc.servus.storage.station;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
 */
public record StationStatePayload(boolean online, String reason, String blockedReason, ItemStack blockedStack,
                                  String blockedZone, boolean idle) implements CustomPacketPayload {

    public static final Type<StationStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "station_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StationStatePayload::online,
            ByteBufCodecs.STRING_UTF8, StationStatePayload::reason,
            ByteBufCodecs.STRING_UTF8, StationStatePayload::blockedReason,
            ItemStack.OPTIONAL_STREAM_CODEC, StationStatePayload::blockedStack,
            ByteBufCodecs.STRING_UTF8, StationStatePayload::blockedZone,
            ByteBufCodecs.BOOL, StationStatePayload::idle,
            StationStatePayload::new);

    public static final StationStatePayload EMPTY = new StationStatePayload(false, "", "", ItemStack.EMPTY, "", true);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
