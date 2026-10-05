package io.github.dirnei.mc.servus.lib.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Nodes at block positions, edges derived from a {@link ConnectionRule}, and
 * connected components with stable integer ids. Every mutation touches only
 * the component it affects and reports what happened as a {@link GraphChange}.
 *
 * <p>Not thread safe. Networks drive it from the server thread only.
 *
 * @param <N> the node type
 */
public final class NetworkGraph<N> {

    private final ConnectionRule<N> rule;
    private final Map<NodePos, N> nodes = new HashMap<>();
    private final Map<NodePos, Set<NodePos>> edges = new HashMap<>();
    private final Map<NodePos, Integer> componentOf = new HashMap<>();
    private final Map<Integer, Set<NodePos>> members = new HashMap<>();
    private int nextId = 1;

    public NetworkGraph(ConnectionRule<N> rule) {
        this.rule = rule;
    }

    // Mutation

    /**
     * Adds a node. Creates a component, joins one, or merges several.
     *
     * @throws IllegalArgumentException when the position already holds a node
     */
    public GraphChange addNode(NodePos pos, N node) {
        if (nodes.containsKey(pos)) {
            throw new IllegalArgumentException("Position already holds a node: " + pos);
        }
        nodes.put(pos, node);

        Set<NodePos> adjacent = new HashSet<>();
        for (Side side : Side.ALL) {
            NodePos other = pos.relative(side);
            N otherNode = nodes.get(other);
            if (otherNode == null) {
                continue;
            }
            if (rule.canConnect(pos, node, side, other, otherNode)
                    && rule.canConnect(other, otherNode, side.opposite(), pos, node)) {
                adjacent.add(other);
                edges.get(other).add(pos);
            }
        }
        edges.put(pos, adjacent);

        Set<Integer> touched = new LinkedHashSet<>();
        for (NodePos a : adjacent) {
            touched.add(componentOf.get(a));
        }

        if (touched.isEmpty()) {
            int id = nextId++;
            Set<NodePos> set = new HashSet<>();
            set.add(pos);
            members.put(id, set);
            componentOf.put(pos, id);
            return new GraphChange(id, List.of(id), List.of(), List.of());
        }

        int survivor = largest(touched);
        List<Integer> merged = new ArrayList<>();
        for (int id : touched) {
            if (id == survivor) {
                continue;
            }
            Set<NodePos> absorbed = members.remove(id);
            for (NodePos p : absorbed) {
                componentOf.put(p, survivor);
            }
            members.get(survivor).addAll(absorbed);
            merged.add(id);
        }
        members.get(survivor).add(pos);
        componentOf.put(pos, survivor);
        return new GraphChange(survivor, List.of(), merged, List.of());
    }

    /**
     * Removes a node. Shrinks, splits or deletes its component.
     *
     * @throws IllegalArgumentException when the position holds no node
     */
    public GraphChange removeNode(NodePos pos) {
        if (!nodes.containsKey(pos)) {
            throw new IllegalArgumentException("Position holds no node: " + pos);
        }
        nodes.remove(pos);
        Set<NodePos> adjacent = edges.remove(pos);
        for (NodePos a : adjacent) {
            edges.get(a).remove(pos);
        }
        int id = componentOf.remove(pos);
        Set<NodePos> set = members.get(id);
        set.remove(pos);

        if (set.isEmpty()) {
            members.remove(id);
            return new GraphChange(GraphChange.NO_COMPONENT, List.of(), List.of(), List.of(id));
        }
        if (adjacent.size() <= 1) {
            // A leaf or an isolated member left; the rest is still connected.
            return GraphChange.survivorOnly(id);
        }

        List<Set<NodePos>> parts = new ArrayList<>();
        Set<NodePos> seen = new HashSet<>();
        for (NodePos start : adjacent) {
            if (!seen.contains(start)) {
                parts.add(collect(start, seen));
            }
        }
        if (parts.size() == 1) {
            return GraphChange.survivorOnly(id);
        }

        parts.sort((a, b) -> Integer.compare(b.size(), a.size()));
        members.put(id, parts.get(0));
        List<Integer> created = new ArrayList<>();
        for (int i = 1; i < parts.size(); i++) {
            int newId = nextId++;
            Set<NodePos> part = parts.get(i);
            members.put(newId, part);
            for (NodePos p : part) {
                componentOf.put(p, newId);
            }
            created.add(newId);
        }
        return new GraphChange(id, created, List.of(), List.of());
    }

    /**
     * Re-evaluates the edges of one node after its state changed, for example
     * when a side was disabled. Reports the net effect on components.
     */
    public GraphChange refresh(NodePos pos) {
        N node = nodes.get(pos);
        if (node == null) {
            throw new IllegalArgumentException("Position holds no node: " + pos);
        }
        GraphChange removed = removeNode(pos);
        GraphChange added = addNode(pos, node);
        return removed.andThen(added);
    }

    // Queries

    public boolean contains(NodePos pos) {
        return nodes.containsKey(pos);
    }

    public Optional<N> node(NodePos pos) {
        return Optional.ofNullable(nodes.get(pos));
    }

    public OptionalInt componentOf(NodePos pos) {
        Integer id = componentOf.get(pos);
        return id == null ? OptionalInt.empty() : OptionalInt.of(id);
    }

    /** Positions in a component, empty when the id is unknown. */
    public Set<NodePos> members(int componentId) {
        Set<NodePos> set = members.get(componentId);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }

    /** Connected neighbours of a position, empty when it holds no node. */
    public Set<NodePos> neighbors(NodePos pos) {
        Set<NodePos> set = edges.get(pos);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }

    public Set<Integer> componentIds() {
        return Collections.unmodifiableSet(members.keySet());
    }

    public int size() {
        return nodes.size();
    }

    // Internals

    private int largest(Set<Integer> ids) {
        int best = -1;
        int bestSize = -1;
        for (int id : ids) {
            int size = members.get(id).size();
            if (size > bestSize || (size == bestSize && id < best)) {
                best = id;
                bestSize = size;
            }
        }
        return best;
    }

    private Set<NodePos> collect(NodePos start, Set<NodePos> seen) {
        Set<NodePos> part = new HashSet<>();
        Deque<NodePos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            NodePos current = queue.poll();
            part.add(current);
            for (NodePos next : edges.get(current)) {
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return part;
    }
}
