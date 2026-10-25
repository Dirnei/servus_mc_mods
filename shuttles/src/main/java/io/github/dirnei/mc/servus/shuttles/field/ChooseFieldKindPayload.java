package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: the field kind chosen on the open Field Head's Field tab, by {@link FieldKind} ordinal. */
public record ChooseFieldKindPayload(int kind) implements CustomPacketPayload {

    public static final Type<ChooseFieldKindPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "choose_field_kind"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseFieldKindPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChooseFieldKindPayload::kind,
            ChooseFieldKindPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
