package io.github.dirnei.mc.servus.lib.network;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.network.core.NetworkSet;
import io.github.dirnei.mc.servus.lib.network.core.ServusNetwork;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Networks of one server level. Owns one {@link NetworkSet} per network type,
 * created on first use, routes node joins and leaves to them and ticks them
 * at the end of every level tick.
 *
 * <p>Server thread only.
 */
public final class NetworkManager {

    private static final Map<ResourceKey<Level>, NetworkManager> MANAGERS = new HashMap<>();

    private final ResourceKey<Level> dimension;
    private final Map<NetworkType<?, ?, ?>, NetworkSet<?, ?, ?>> sets = new HashMap<>();

    private NetworkManager(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public static NetworkManager of(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level.dimension(), NetworkManager::new);
    }

    static Optional<NetworkManager> existing(ServerLevel level) {
        return Optional.ofNullable(MANAGERS.get(level.dimension()));
    }

    static void unload(ServerLevel level) {
        NetworkManager manager = MANAGERS.remove(level.dimension());
        if (manager != null) {
            manager.disposeAll();
        }
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    @SuppressWarnings("unchecked")
    public <N, E, T extends ServusNetwork<N, E>> NetworkSet<N, E, T> set(NetworkType<N, E, T> type) {
        return (NetworkSet<N, E, T>) sets.computeIfAbsent(type, t -> type.createSet());
    }

    /** Adds a node to its network type. Returns the network it ended up in. */
    public <N> ServusNetwork<N, ?> join(BlockPos pos, NetworkNode<N> node) {
        return joinTyped(node.networkType(), Conversions.toNodePos(pos), node.networkNode());
    }

    /** Removes a node. No-op when the position is not part of that network type. */
    public <N> void leave(BlockPos pos, NetworkNode<N> node) {
        NetworkSet<N, ?, ?> set = set(node.networkType());
        NodePos nodePos = Conversions.toNodePos(pos);
        if (set.graph().contains(nodePos)) {
            set.nodeRemoved(nodePos);
        }
    }

    /** Re-evaluates a node's connections after its state changed. */
    public <N> void refresh(BlockPos pos, NetworkNode<N> node) {
        NetworkSet<N, ?, ?> set = set(node.networkType());
        NodePos nodePos = Conversions.toNodePos(pos);
        if (set.graph().contains(nodePos)) {
            set.nodeRefreshed(nodePos);
        }
    }

    /** Ticks every network set, in network type registration order. */
    public void tick() {
        for (NetworkType<?, ?, ?> type : NetworkTypes.all()) {
            NetworkSet<?, ?, ?> set = sets.get(type);
            if (set != null) {
                set.tick();
            }
        }
    }

    public void disposeAll() {
        for (NetworkSet<?, ?, ?> set : sets.values()) {
            set.disposeAll();
        }
        sets.clear();
    }

    private <N, E, T extends ServusNetwork<N, E>> T joinTyped(NetworkType<N, E, T> type, NodePos pos, N node) {
        return set(type).nodeAdded(pos, node);
    }
}
