package io.github.dirnei.mc.servus.storage.board;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what the open order board shows.
 *
 * @param online   whether the warehouse is online
 * @param reason   why not, when offline
 * @param machines every dock with its machine
 * @param orders   every work order
 */
public record OrderBoardPayload(boolean online, String reason, List<MachineRow> machines, List<OrderRow> orders)
        implements CustomPacketPayload {

    public static final Type<OrderBoardPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "order_board"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrderBoardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OrderBoardPayload::online,
            ByteBufCodecs.STRING_UTF8, OrderBoardPayload::reason,
            MachineRow.STREAM_CODEC.apply(ByteBufCodecs.list()), OrderBoardPayload::machines,
            OrderRow.STREAM_CODEC.apply(ByteBufCodecs.list()), OrderBoardPayload::orders,
            OrderBoardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
