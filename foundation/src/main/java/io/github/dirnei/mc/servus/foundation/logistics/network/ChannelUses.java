package io.github.dirnei.mc.servus.foundation.logistics.network;

import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Where duct channels are in use, from the loaded item, power and fluid line networks. Server thread only. */
public final class ChannelUses {

    private static final List<NetworkType<UtilityDuctBlockEntity, DuctEvent, ? extends LineNetwork<?>>> TYPES =
            List.of(ItemChannelNetwork.TYPE, EnergyChannelNetwork.TYPE, FluidChannelNetwork.TYPE);

    private ChannelUses() {
    }

    /** The number of loaded duct sides on each channel id, over every line network of every level. */
    public static Map<Integer, Integer> loaded(MinecraftServer server) {
        Map<Integer, Integer> uses = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            NetworkManager manager = NetworkManager.of(level);
            for (NetworkType<UtilityDuctBlockEntity, DuctEvent, ? extends LineNetwork<?>> type : TYPES) {
                for (LineNetwork<?> network : manager.set(type).networks()) {
                    network.channelUses().forEach((id, count) -> uses.merge(id, count, Integer::sum));
                }
            }
        }
        return uses;
    }

    /** The channel uses on the connected run of the duct's line; empty for the data line or without a network. */
    public static Map<Integer, Integer> onCable(UtilityDuctBlockEntity duct, Line line) {
        if (line == Line.DATA || !duct.has(line)) {
            return Map.of();
        }
        return duct.<LineNetwork<?>>network(line).map(LineNetwork::channelUses).orElse(Map.of());
    }
}
