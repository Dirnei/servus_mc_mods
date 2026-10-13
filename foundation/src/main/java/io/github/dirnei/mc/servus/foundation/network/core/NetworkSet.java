package io.github.dirnei.mc.servus.foundation.network.core;

import io.github.dirnei.mc.servus.foundation.graph.ConnectionRule;
import io.github.dirnei.mc.servus.foundation.graph.GraphChange;
import io.github.dirnei.mc.servus.foundation.graph.NetworkGraph;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
 * All networks of one type in one level: a graph of nodes plus one network
 * object per connected component. Keeps the two in step and ticks the
 * networks in ascending id order.
 *
 * <p>Not thread safe. Drive it from the server thread only.
 *
 * @param <N> node type
 * @param <E> event type
 * @param <T> network type
 */
public final class NetworkSet<N, E, T extends ServusNetwork<N, E>> {

    private final NetworkGraph<N> graph;
    private final NetworkFactory<N, E, T> factory;
    private final TreeMap<Integer, T> networks = new TreeMap<>();

    public NetworkSet(ConnectionRule<N> rule, NetworkFactory<N, E, T> factory) {
        this.graph = new NetworkGraph<>(rule);
        this.factory = factory;
    }

    public NetworkGraph<N> graph() {
        return graph;
    }

    public Optional<T> networkAt(NodePos pos) {
        var id = graph.componentOf(pos);
        return id.isEmpty() ? Optional.empty() : Optional.ofNullable(networks.get(id.getAsInt()));
    }

    public Optional<T> network(int id) {
        return Optional.ofNullable(networks.get(id));
    }

    /** Networks in ascending id order. Read only snapshot. */
    public Collection<T> networks() {
        return Collections.unmodifiableCollection(networks.values());
    }

    /** A node appeared. Creates, joins or merges networks accordingly. */
    public T nodeAdded(NodePos pos, N node) {
        GraphChange change = graph.addNode(pos, node);
        T survivor = apply(change);
        survivor.onNodeAdded(pos, node);
        return survivor;
    }

    /** A node went away. Shrinks, splits or disposes networks accordingly. */
    public void nodeRemoved(NodePos pos) {
        N node = graph.node(pos).orElseThrow(() -> new IllegalArgumentException("No node at " + pos));
        T old = networkAt(pos).orElseThrow();
        old.onNodeRemoved(pos, node);
        GraphChange change = graph.removeNode(pos);
        apply(change);
        handOver(old, change);
    }

    /** A node changed in a way that affects its connections. */
    public T nodeRefreshed(NodePos pos) {
        N node = graph.node(pos).orElseThrow(() -> new IllegalArgumentException("No node at " + pos));
        T old = networkAt(pos).orElseThrow();
        old.onNodeRemoved(pos, node);
        GraphChange change = graph.refresh(pos);
        T survivor = apply(change);
        handOver(old, change);
        survivor.onNodeAdded(pos, node);
        return survivor;
    }

    /**
     * After a split, every node that ended up in a newly created network is
     * told to leave the old one and to join the new one, so both keep their
     * state in step with the graph.
     */
    private void handOver(T old, GraphChange change) {
        for (int id : change.created()) {
            T created = networks.get(id);
            if (created == null) {
                continue;
            }
            for (NodePos member : new ArrayList<>(graph.members(id))) {
                N node = graph.node(member).orElseThrow();
                if (!old.isDisposed()) {
                    old.onNodeRemoved(member, node);
                }
                created.onNodeAdded(member, node);
            }
        }
    }

    private T apply(GraphChange change) {
        for (int id : change.created()) {
            networks.put(id, factory.create(id, this));
        }
        T survivor = change.survivor() == GraphChange.NO_COMPONENT ? null : networks.get(change.survivor());
        for (int id : change.merged()) {
            T absorbed = networks.remove(id);
            if (absorbed != null) {
                if (survivor != null) {
                    survivor.onAbsorb(absorbed);
                }
                absorbed.markDisposed();
            }
        }
        for (int id : change.removed()) {
            T removed = networks.remove(id);
            if (removed != null) {
                removed.markDisposed();
            }
        }
        return survivor;
    }

    /** Drains every mailbox and ticks every network, in ascending id order. */
    public void tick() {
        List<T> snapshot = new ArrayList<>(networks.values());
        for (T network : snapshot) {
            if (network.isDisposed()) {
                continue;
            }
            network.mailbox().drain(network::handle);
            network.onTick();
        }
    }

    /** Disposes every network and forgets every node. Used on level unload. */
    public void disposeAll() {
        List<T> all = new ArrayList<>(networks.values());
        networks.clear();
        for (T network : all) {
            network.markDisposed();
        }
        for (NodePos pos : new ArrayList<>(allPositions())) {
            graph.removeNode(pos);
        }
    }

    private List<NodePos> allPositions() {
        List<NodePos> positions = new ArrayList<>();
        for (int id : graph.componentIds()) {
            positions.addAll(graph.members(id));
        }
        return positions;
    }
}
