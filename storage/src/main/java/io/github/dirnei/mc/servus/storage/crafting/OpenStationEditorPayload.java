package io.github.dirnei.mc.servus.storage.crafting;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: open the recipe editor of the Crafting Station whose menu is open, prefilled with this recipe. */
public record OpenStationEditorPayload(ResourceLocation recipe) implements CustomPacketPayload {

    public static final Type<OpenStationEditorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "open_station_editor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenStationEditorPayload> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(OpenStationEditorPayload::new, OpenStationEditorPayload::recipe).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
