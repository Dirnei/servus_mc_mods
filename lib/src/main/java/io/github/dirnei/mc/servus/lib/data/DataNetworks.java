package io.github.dirnei.mc.servus.lib.data;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.network.NetworkManager;
import io.github.dirnei.mc.servus.lib.network.core.NetworkSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Finds data networks for other mods. Server thread only. */
public final class DataNetworks {

    private DataNetworks() {
    }

    /**
     * The network of the cable at {@code pos}, else the network (lowest id
     * first) that lists {@code pos} as a member, else empty.
     */
    public static Optional<DataNetwork> networkAt(ServerLevel level, BlockPos pos) {
        NetworkSet<DataCableBlockEntity, DataEvent, DataNetwork> set = NetworkManager.of(level).set(DataNetwork.TYPE);
        NodePos node = Conversions.toNodePos(pos);
        Optional<DataNetwork> cable = set.networkAt(node);
        if (cable.isPresent()) {
            return cable;
        }
        return set.networks().stream().filter(network -> network.hasMember(pos)).findFirst();
    }
}
