package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the lift car of the shaft whose lowest segment is at
 * bottom. The client moves the car from y towards target the way the server
 * does, speeding up and braking by drive; a top speed of 0 means it stands still.
 */
public record LiftSyncPayload(BlockPos bottom, int topY, float y, int target, ShuttleView.Drive drive, boolean carrying)
        implements CustomPacketPayload {

    public static final Type<LiftSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "lift_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LiftSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LiftSyncPayload::bottom,
            ByteBufCodecs.VAR_INT, LiftSyncPayload::topY,
            ByteBufCodecs.FLOAT, LiftSyncPayload::y,
            ByteBufCodecs.VAR_INT, LiftSyncPayload::target,
            ShuttleView.Drive.STREAM_CODEC, LiftSyncPayload::drive,
            ByteBufCodecs.BOOL, LiftSyncPayload::carrying,
            LiftSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
