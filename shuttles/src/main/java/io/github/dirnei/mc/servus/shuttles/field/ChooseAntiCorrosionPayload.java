package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: the anti-corrosion item chosen in the open Field Head's Water tab, by item id; empty for Off. */
public record ChooseAntiCorrosionPayload(Optional<Identifier> item) implements CustomPacketPayload {

    public static final Type<ChooseAntiCorrosionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "choose_anti_corrosion"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseAntiCorrosionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), ChooseAntiCorrosionPayload::item,
            ChooseAntiCorrosionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
