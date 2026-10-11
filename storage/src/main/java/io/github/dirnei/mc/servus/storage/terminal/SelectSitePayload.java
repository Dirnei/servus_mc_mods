package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the open Storage Terminal shows this site; empty for its own. */
public record SelectSitePayload(String site) implements CustomPacketPayload {

    public static final Type<SelectSitePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "select_site"));

    public static final StreamCodec<ByteBuf, SelectSitePayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(256).map(SelectSitePayload::new, SelectSitePayload::site);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
