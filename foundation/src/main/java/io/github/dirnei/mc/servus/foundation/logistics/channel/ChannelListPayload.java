package io.github.dirnei.mc.servus.foundation.logistics.channel;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: every duct channel of every team, with the id counter. */
public record ChannelListPayload(List<Channel> channels, int nextId) implements CustomPacketPayload {

    public static final Type<ChannelListPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "channel_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChannelListPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.channels().size());
                for (Channel channel : payload.channels()) {
                    buf.writeVarInt(channel.id());
                    UUIDUtil.STREAM_CODEC.encode(buf, channel.team());
                    buf.writeUtf(channel.name(), ChannelList.MAX_NAME * 4);
                    buf.writeVarInt(channel.colour());
                    buf.writeUtf(channel.icon() == null ? "" : channel.icon());
                }
                buf.writeVarInt(payload.nextId());
            },
            buf -> {
                int size = buf.readVarInt();
                List<Channel> channels = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    int id = buf.readVarInt();
                    channels.add(new Channel(id, UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(ChannelList.MAX_NAME * 4),
                            buf.readVarInt(), emptyToNull(buf.readUtf())));
                }
                return new ChannelListPayload(channels, buf.readVarInt());
            });

    public static ChannelListPayload of(ChannelList list) {
        return new ChannelListPayload(List.copyOf(list.all()), list.nextId());
    }

    public ChannelList toList() {
        return new ChannelList(channels, nextId);
    }

    private static String emptyToNull(String icon) {
        return icon.isEmpty() ? null : icon;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
