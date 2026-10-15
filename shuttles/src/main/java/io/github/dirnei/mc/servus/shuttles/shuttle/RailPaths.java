package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** Breadth first search over rail nodes. Pure Java. */
public final class RailPaths {

    private RailPaths() {
    }

    /**
     * @param start      where the search begins; included as the first node of the result
     * @param goal       which nodes count as a destination
     * @param neighbours the travel edges: only rail and elevator nodes should be returned
     * @return the shortest node list from start to the first goal found, or an empty list when none is reachable
     */
    public static List<NodePos> find(NodePos start, Predicate<NodePos> goal, Function<NodePos, Set<NodePos>> neighbours) {
        if (goal.test(start)) {
            return List.of(start);
        }
        Map<NodePos, NodePos> cameFrom = new HashMap<>();
        ArrayDeque<NodePos> queue = new ArrayDeque<>();
        cameFrom.put(start, start);
        queue.add(start);
        while (!queue.isEmpty()) {
            NodePos current = queue.poll();
            for (NodePos next : neighbours.apply(current)) {
                if (cameFrom.containsKey(next)) {
                    continue;
                }
                cameFrom.put(next, current);
                if (goal.test(next)) {
                    return unwind(cameFrom, start, next);
                }
                queue.add(next);
            }
        }
        return List.of();
    }

    private static List<NodePos> unwind(Map<NodePos, NodePos> cameFrom, NodePos start, NodePos end) {
        List<NodePos> path = new ArrayList<>();
        NodePos node = end;
        while (!node.equals(start)) {
            path.add(node);
            node = cameFrom.get(node);
        }
        path.add(start);
        Collections.reverse(path);
        return path;
    }
}
