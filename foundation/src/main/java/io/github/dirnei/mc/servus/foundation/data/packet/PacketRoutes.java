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

/** Shortest routes over the data nodes of a network. Pure, no Minecraft imports. */
public final class PacketRoutes {

    private static final Comparator<NodePos> ORDER =
            Comparator.comparingInt(NodePos::x).thenComparingInt(NodePos::y).thenComparingInt(NodePos::z);

    private PacketRoutes() {
    }

    /**
     * The shortest path from any start to any goal, both ends included, by
     * breadth first search. Ties resolve the same way every time.
     */
    public static Optional<List<NodePos>> shortest(Set<NodePos> starts, Set<NodePos> goals,
                                                   Function<NodePos, Set<NodePos>> neighbours) {
        if (starts.isEmpty() || goals.isEmpty()) {
            return Optional.empty();
        }
        Map<NodePos, NodePos> previous = new HashMap<>();
        Deque<NodePos> queue = new ArrayDeque<>();
        for (NodePos start : sorted(starts)) {
            previous.put(start, start);
            queue.add(start);
        }
        while (!queue.isEmpty()) {
            NodePos at = queue.poll();
            if (goals.contains(at)) {
                return Optional.of(path(previous, at));
            }
            for (NodePos next : sorted(neighbours.apply(at))) {
                if (!previous.containsKey(next)) {
                    previous.put(next, at);
                    queue.add(next);
                }
            }
        }
        return Optional.empty();
    }

    private static List<NodePos> sorted(Set<NodePos> positions) {
        List<NodePos> list = new ArrayList<>(positions);
        list.sort(ORDER);
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
