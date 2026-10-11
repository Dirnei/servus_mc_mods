package io.github.dirnei.mc.servus.storage.controller;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: the name typed into the open controller GUI. */
public record RenameSitePayload(String name) implements CustomPacketPayload {

    public static final Type<RenameSitePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "rename_site"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, RenameSitePayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(64).map(RenameSitePayload::new, RenameSitePayload::name);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
