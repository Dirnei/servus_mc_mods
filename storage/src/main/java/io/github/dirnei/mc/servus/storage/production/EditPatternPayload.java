package io.github.dirnei.mc.servus.storage.production;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: open the Production Terminal's editor on the pattern in this slot of this Pattern Storage. */
public record EditPatternPayload(BlockPos storage, int slot) implements CustomPacketPayload {

    public static final Type<EditPatternPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "edit_pattern"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EditPatternPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EditPatternPayload::storage,
            ByteBufCodecs.VAR_INT, EditPatternPayload::slot,
            EditPatternPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
