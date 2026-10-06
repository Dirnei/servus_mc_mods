package io.github.dirnei.mc.servus.storage.zone;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: set the tags of the Zone Label open in the label menu from a comma separated line. */
public record SetLabelTagsPayload(String line) implements CustomPacketPayload {

    public static final Type<SetLabelTagsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "set_label_tags"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetLabelTagsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetLabelTagsPayload::line,
            SetLabelTagsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
