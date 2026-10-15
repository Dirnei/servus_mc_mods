package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the shuttles of one garage. */
public record ShuttleSyncPayload(BlockPos garage, List<ShuttleView> shuttles) implements CustomPacketPayload {

    public static final Type<ShuttleSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "shuttle_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShuttleSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShuttleSyncPayload::garage,
            ShuttleView.STREAM_CODEC.apply(ByteBufCodecs.list()), ShuttleSyncPayload::shuttles,
            ShuttleSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
