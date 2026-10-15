package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: a number typed on the Stock tab of the open Field Head
 * GUI. {@code which} is a {@link FieldAmount} ordinal; the server clamps
 * {@code value}.
 */
public record SetFieldAmountPayload(BlockPos pos, int which, long value) implements CustomPacketPayload {

    public static final Type<SetFieldAmountPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "set_field_amount"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFieldAmountPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetFieldAmountPayload::pos,
            ByteBufCodecs.VAR_INT, SetFieldAmountPayload::which,
            ByteBufCodecs.VAR_LONG, SetFieldAmountPayload::value,
            SetFieldAmountPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
