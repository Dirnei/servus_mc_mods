package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: the shuttles of the warehouse behind this controller. */
public record ShuttleSyncPayload(BlockPos controller, List<ShuttleView> shuttles) implements CustomPacketPayload {

    public static final Type<ShuttleSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "shuttle_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShuttleSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShuttleSyncPayload::controller,
            ShuttleView.STREAM_CODEC.apply(ByteBufCodecs.list()), ShuttleSyncPayload::shuttles,
            ShuttleSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
