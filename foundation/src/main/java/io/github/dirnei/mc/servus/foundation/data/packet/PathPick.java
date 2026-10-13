package io.github.dirnei.mc.servus.foundation.data.packet;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Picks the route of a delivery: among the shortest paths from any start to
 * any goal, the one whose slowest node has the most bandwidth. No max flow,
 * no detours for bandwidth. Ties resolve the same way every time.
 */
public final class PathPick {

    /** The fixed order ties resolve in: by x, then y, then z. */
    public static final Comparator<NodePos> NODE_ORDER =
            Comparator.comparingInt(NodePos::x).thenComparingInt(NodePos::y).thenComparingInt(NodePos::z);

    private PathPick() {
    }

    /**
     * The best path, both ends included.
     *
     * @param bandwidth items per second a node lets through; use
     *                  {@link Double#POSITIVE_INFINITY} for a node without a limit
     */
    public static Optional<List<NodePos>> best(Set<NodePos> starts, Set<NodePos> goals,
                                               Function<NodePos, Set<NodePos>> neighbours,
                                               ToDoubleFunction<NodePos> bandwidth) {
        if (starts.isEmpty() || goals.isEmpty()) {
            return Optional.empty();
        }
        // Breadth first by layers; each node keeps the predecessor with the widest bottleneck so far.
        Map<NodePos, Integer> distance = new HashMap<>();
        Map<NodePos, Double> bottleneck = new HashMap<>();
        Map<NodePos, NodePos> previous = new HashMap<>();
        List<NodePos> layer = new ArrayList<>();
        for (NodePos start : sorted(starts)) {
            distance.put(start, 0);
            bottleneck.put(start, bandwidth.applyAsDouble(start));
            previous.put(start, start);
            layer.add(start);
        }
        int depth = 0;
        while (!layer.isEmpty()) {
            NodePos reached = null;
            for (NodePos at : layer) {
                if (goals.contains(at) && (reached == null || bottleneck.get(at) > bottleneck.get(reached))) {
                    reached = at;
                }
            }
            if (reached != null) {
                return Optional.of(path(previous, reached));
            }
            depth++;
            Deque<NodePos> next = new ArrayDeque<>();
            for (NodePos at : layer) {
                double through = bottleneck.get(at);
                for (NodePos neighbour : sorted(neighbours.apply(at))) {
                    Integer known = distance.get(neighbour);
                    double width = Math.min(through, bandwidth.applyAsDouble(neighbour));
                    if (known == null) {
                        distance.put(neighbour, depth);
                        bottleneck.put(neighbour, width);
                        previous.put(neighbour, at);
                        next.add(neighbour);
                    } else if (known == depth && width > bottleneck.get(neighbour)) {
                        bottleneck.put(neighbour, width);
                        previous.put(neighbour, at);
                    }
                }
            }
            layer = new ArrayList<>(next);
        }
        return Optional.empty();
    }

    private static List<NodePos> sorted(Set<NodePos> positions) {
        List<NodePos> list = new ArrayList<>(positions);
        list.sort(NODE_ORDER);
        return list;
    }

    private static List<NodePos> path(Map<NodePos, NodePos> previous, NodePos end) {
        List<NodePos> path = new ArrayList<>();
        NodePos at = end;
        while (true) {
            path.add(at);
            NodePos before = previous.get(at);
            if (before.equals(at)) {
                break;
            }
            at = before;
        }
        Collections.reverse(path);
        return path;
    }
}
