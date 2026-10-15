package io.github.dirnei.mc.servus.storage.field;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: the crop chosen in the open Field Head's picker, by item id; empty for harvest only. */
public record ChooseFieldCropPayload(Optional<Identifier> crop) implements CustomPacketPayload {

    public static final Type<ChooseFieldCropPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "choose_field_crop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseFieldCropPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), ChooseFieldCropPayload::crop,
            ChooseFieldCropPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
