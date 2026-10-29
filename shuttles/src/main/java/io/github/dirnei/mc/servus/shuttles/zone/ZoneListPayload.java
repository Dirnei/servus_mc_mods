package io.github.dirnei.mc.servus.shuttles.zone;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: every warehouse zone of every team, with the id counter. */
public record ZoneListPayload(List<ZoneList.Zone<ZoneFilter>> zones, int nextId) implements CustomPacketPayload {

    public static final Type<ZoneListPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "zone_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ZoneListPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.zones().size());
                for (ZoneList.Zone<ZoneFilter> zone : payload.zones()) {
                    buf.writeVarInt(zone.id());
                    UUIDUtil.STREAM_CODEC.encode(buf, zone.team());
                    buf.writeUtf(zone.name(), ZoneList.MAX_NAME * 4);
                    buf.writeVarInt(zone.colour());
                    ZoneFilter.STREAM_CODEC.encode(buf, zone.filter());
                }
                buf.writeVarInt(payload.nextId());
            },
            buf -> {
                int size = buf.readVarInt();
                List<ZoneList.Zone<ZoneFilter>> zones = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    zones.add(new ZoneList.Zone<>(buf.readVarInt(), UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(ZoneList.MAX_NAME * 4),
                            buf.readVarInt(), ZoneFilter.STREAM_CODEC.decode(buf)));
                }
                return new ZoneListPayload(zones, buf.readVarInt());
            });

    public static ZoneListPayload of(ZoneList<ZoneFilter> list) {
        return new ZoneListPayload(List.copyOf(list.all()), list.nextId());
    }

    public ZoneList<ZoneFilter> toList() {
        return new ZoneList<>(zones, nextId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
