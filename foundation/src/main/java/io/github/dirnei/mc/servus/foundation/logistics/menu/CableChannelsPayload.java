package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client, for the open endpoint GUI: the player's team, per line
 * the channel ids used on the connected run of the side's duct, and how many
 * loaded sides use each of the team's channels.
 *
 * @param team       the player's team
 * @param onCable    channel ids used on the run, indexed by {@link Line#ordinal()}
 * @param loadedUses loaded sides per channel id of the team's channels
 */
public record CableChannelsPayload(UUID team, List<Set<Integer>> onCable, Map<Integer, Integer> loadedUses)
        implements CustomPacketPayload {

    public static final Type<CableChannelsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "cable_channels"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CableChannelsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, payload.team());
                buf.writeVarInt(payload.onCable().size());
                for (Set<Integer> ids : payload.onCable()) {
                    buf.writeVarInt(ids.size());
                    ids.forEach(buf::writeVarInt);
                }
                buf.writeVarInt(payload.loadedUses().size());
                payload.loadedUses().forEach((id, uses) -> {
                    buf.writeVarInt(id);
                    buf.writeVarInt(uses);
                });
            },
            buf -> {
                UUID team = UUIDUtil.STREAM_CODEC.decode(buf);
                int lines = buf.readVarInt();
                List<Set<Integer>> onCable = new ArrayList<>(lines);
                for (int line = 0; line < lines; line++) {
                    int size = buf.readVarInt();
                    Set<Integer> ids = new HashSet<>();
                    for (int i = 0; i < size; i++) {
                        ids.add(buf.readVarInt());
                    }
                    onCable.add(ids);
                }
                int size = buf.readVarInt();
                Map<Integer, Integer> uses = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    uses.put(buf.readVarInt(), buf.readVarInt());
                }
                return new CableChannelsPayload(team, onCable, uses);
            });

    /** The channel ids used on the run of that line; empty when unknown. */
    public Set<Integer> onCable(Line line) {
        return line.ordinal() < onCable.size() ? onCable.get(line.ordinal()) : Set.of();
    }

    public int loadedUses(int id) {
        return loadedUses.getOrDefault(id, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
