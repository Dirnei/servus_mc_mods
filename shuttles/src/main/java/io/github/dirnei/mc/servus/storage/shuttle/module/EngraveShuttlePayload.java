package io.github.dirnei.mc.servus.storage.shuttle.module;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: finish the build by hand in the open Shuttle Workshop and engrave this name. */
public record EngraveShuttlePayload(String name) implements CustomPacketPayload {

    public static final Type<EngraveShuttlePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "engrave_shuttle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EngraveShuttlePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), EngraveShuttlePayload::name,
            EngraveShuttlePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
