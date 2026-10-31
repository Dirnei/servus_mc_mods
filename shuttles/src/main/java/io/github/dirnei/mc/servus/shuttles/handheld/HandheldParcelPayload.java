package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: a parcel of the player is ready; the Handheld panel
 * names the locker and the boxes until the next order or collection.
 *
 * @param locker the locker's label
 * @param boxes  the boxes holding the parcel, such as {@code S-01, L-01}
 */
public record HandheldParcelPayload(Component locker, String boxes) implements CustomPacketPayload {

    public static final Type<HandheldParcelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_parcel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandheldParcelPayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, HandheldParcelPayload::locker,
            ByteBufCodecs.STRING_UTF8, HandheldParcelPayload::boxes,
            HandheldParcelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
