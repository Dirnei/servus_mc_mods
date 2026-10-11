package io.github.dirnei.mc.servus.storage.field;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the fertilizer chosen in the open Field Head's Fertilizer tab, by item id; empty for Off. */
public record ChooseFertilizerPayload(Optional<ResourceLocation> fertilizer) implements CustomPacketPayload {

    public static final Type<ChooseFertilizerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "choose_fertilizer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseFertilizerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), ChooseFertilizerPayload::fertilizer,
            ChooseFertilizerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
