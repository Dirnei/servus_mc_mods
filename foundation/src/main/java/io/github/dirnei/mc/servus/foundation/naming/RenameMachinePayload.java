package io.github.dirnei.mc.servus.foundation.naming;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: the name typed into the title row of the open machine GUI. */
public record RenameMachinePayload(String name) implements CustomPacketPayload {

    public static final Type<RenameMachinePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "rename_machine"));

    public static final StreamCodec<ByteBuf, RenameMachinePayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(64).map(RenameMachinePayload::new, RenameMachinePayload::name);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
