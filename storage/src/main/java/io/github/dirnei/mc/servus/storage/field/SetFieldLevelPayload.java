package io.github.dirnei.mc.servus.storage.field;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: a step button of a stock level in the open Field Head GUI.
 * {@code which} is a {@link StockLevels.Level} ordinal, {@code delta} one of the
 * menu's level steps, and {@code shift} moves ten times as far.
 */
public record SetFieldLevelPayload(BlockPos pos, int which, int delta, boolean shift) implements CustomPacketPayload {

    public static final Type<SetFieldLevelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "set_field_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFieldLevelPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetFieldLevelPayload::pos,
            ByteBufCodecs.VAR_INT, SetFieldLevelPayload::which,
            ByteBufCodecs.VAR_INT, SetFieldLevelPayload::delta,
            ByteBufCodecs.BOOL, SetFieldLevelPayload::shift,
            SetFieldLevelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
