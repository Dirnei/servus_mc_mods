package io.github.dirnei.mc.servus.notes.network;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: whether FTB Teams runs and the party the player is in, for the Team tab and the Share popup. */
public record ViewerPayload(boolean teamsInstalled, Optional<UUID> party) implements CustomPacketPayload {

    public static final Type<ViewerPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "viewer"));

    public static final StreamCodec<FriendlyByteBuf, ViewerPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.teamsInstalled());
                buf.writeOptional(payload.party(), UUIDUtil.STREAM_CODEC);
            },
            buf -> new ViewerPayload(buf.readBoolean(), buf.readOptional(UUIDUtil.STREAM_CODEC)));

    @Override
    public Type<ViewerPayload> type() {
        return TYPE;
    }
}
