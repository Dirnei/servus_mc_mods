package io.github.dirnei.mc.servus.foundation.network;

import io.github.dirnei.mc.servus.foundation.graph.ConnectionRule;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkFactory;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import net.minecraft.resources.Identifier;

/**
 * One kind of network: duct, cable, warehouse. Content mods create one per
 * kind and register it with {@link NetworkTypes#register(NetworkType)} in
 * their mod constructor.
 *
 * @param <N> node type stored in the graph
 * @param <E> event type delivered to networks
 * @param <T> network class
 */
public final class NetworkType<N, E, T extends ServusNetwork<N, E>> {

    private final Identifier id;
    private final ConnectionRule<N> rule;
    private final NetworkFactory<N, E, T> factory;

    public NetworkType(Identifier id, ConnectionRule<N> rule, NetworkFactory<N, E, T> factory) {
        this.id = id;
        this.rule = rule;
        this.factory = factory;
    }

    public Identifier id() {
        return id;
    }

    public ConnectionRule<N> rule() {
        return rule;
    }

    public NetworkFactory<N, E, T> factory() {
        return factory;
    }

    NetworkSet<N, E, T> createSet() {
        return new NetworkSet<>(rule, factory);
    }

    @Override
    public String toString() {
        return "NetworkType[" + id + "]";
    }
}
