package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: the crop or sapling chosen in the open Field Head's
 * picker, by item id; empty for harvest only or fell only. {@code twoByTwo}
 * picks a sapling's 2x2 entry.
 */
public record ChooseFieldCropPayload(Optional<Identifier> crop, boolean twoByTwo) implements CustomPacketPayload {

    public static final Type<ChooseFieldCropPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "choose_field_crop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseFieldCropPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), ChooseFieldCropPayload::crop,
            ByteBufCodecs.BOOL, ChooseFieldCropPayload::twoByTwo,
            ChooseFieldCropPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
