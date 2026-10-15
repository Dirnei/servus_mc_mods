package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RailPathsTest {

    private static NodePos n(int x, int z) {
        return new NodePos(x, 0, z);
    }

    /** An undirected graph from edge pairs. */
    private static Map<NodePos, Set<NodePos>> graph(NodePos... pairs) {
        Map<NodePos, Set<NodePos>> edges = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            edges.computeIfAbsent(pairs[i], k -> new HashSet<>()).add(pairs[i + 1]);
            edges.computeIfAbsent(pairs[i + 1], k -> new HashSet<>()).add(pairs[i]);
        }
        return edges;
    }

    @Test
    void straightLine() {
        Map<NodePos, Set<NodePos>> g = graph(n(0, 0), n(1, 0), n(1, 0), n(2, 0), n(2, 0), n(3, 0));
        List<NodePos> path = RailPaths.find(n(0, 0), pos -> pos.equals(n(3, 0)), pos -> g.getOrDefault(pos, Set.of()));
        assertEquals(List.of(n(0, 0), n(1, 0), n(2, 0), n(3, 0)), path);
    }

    @Test
    void branchPicksTheShorter() {
        // 0 -> 1 -> 2 -> goal, and 0 -> a -> b -> c -> goal
        Map<NodePos, Set<NodePos>> g = graph(
                n(0, 0), n(1, 0), n(1, 0), n(2, 0), n(2, 0), n(3, 0),
                n(0, 0), n(0, 1), n(0, 1), n(0, 2), n(0, 2), n(0, 3), n(0, 3), n(3, 0));
        List<NodePos> path = RailPaths.find(n(0, 0), pos -> pos.equals(n(3, 0)), pos -> g.getOrDefault(pos, Set.of()));
        assertEquals(4, path.size());
        assertEquals(n(3, 0), path.get(3));
    }

    @Test
    void unreachableGivesEmpty() {
        Map<NodePos, Set<NodePos>> g = graph(n(0, 0), n(1, 0));
        assertTrue(RailPaths.find(n(0, 0), pos -> pos.equals(n(5, 5)), pos -> g.getOrDefault(pos, Set.of())).isEmpty());
    }

    @Test
    void startIsGoal() {
        assertEquals(List.of(n(0, 0)), RailPaths.find(n(0, 0), pos -> true, pos -> Set.of()));
    }
}
