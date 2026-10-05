package io.github.dirnei.mc.servus.manufacturing.network;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: add this recipe to the assembler whose menu the player has open. */
public record ConfigureRecipePayload(ResourceLocation recipe) implements CustomPacketPayload {

    public static final Type<ConfigureRecipePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "configure_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureRecipePayload> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(ConfigureRecipePayload::new, ConfigureRecipePayload::recipe).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
