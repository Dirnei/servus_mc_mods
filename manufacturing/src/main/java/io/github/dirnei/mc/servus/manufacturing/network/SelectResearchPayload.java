package io.github.dirnei.mc.servus.manufacturing.network;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the player wants this tech to be the team's research target. */
public record SelectResearchPayload(ResourceLocation tech) implements CustomPacketPayload {

    public static final Type<SelectResearchPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "select_research"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectResearchPayload> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(SelectResearchPayload::new, SelectResearchPayload::tech).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
