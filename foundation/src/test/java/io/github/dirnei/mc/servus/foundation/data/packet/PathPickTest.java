package io.github.dirnei.mc.servus.foundation.data.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PathPickTest {

    /** A grid graph of the given nodes, linked when they touch. */
    private static Set<NodePos> neighbours(Set<NodePos> nodes, NodePos at) {
        Set<NodePos> result = new HashSet<>();
        for (Side side : Side.ALL) {
            NodePos next = at.relative(side);
            if (nodes.contains(next)) {
                result.add(next);
            }
        }
        return result;
    }

    private static NodePos p(int x, int z) {
        return new NodePos(x, 0, z);
    }

    @Test
    void shortestWins() {
        // A straight run of three slow ducts and a detour of five fast ones: the short run wins.
        Set<NodePos> nodes = new HashSet<>(List.of(p(0, 0), p(1, 0), p(2, 0), p(3, 0), p(4, 0),
                p(0, 1), p(0, 2), p(1, 2), p(2, 2), p(3, 2), p(4, 2), p(4, 1)));
        Map<NodePos, Double> bandwidth = new HashMap<>();
        nodes.forEach(node -> bandwidth.put(node, 128.0));
        bandwidth.put(p(2, 0), 32.0);
        List<NodePos> path = PathPick.best(Set.of(p(0, 0)), Set.of(p(4, 0)), at -> neighbours(nodes, at),
                at -> bandwidth.getOrDefault(at, Double.POSITIVE_INFINITY)).orElseThrow();
        assertEquals(List.of(p(0, 0), p(1, 0), p(2, 0), p(3, 0), p(4, 0)), path);
        assertEquals(32.0, PathPick.bottleneck(path, bandwidth::get), 1e-9);
    }

    @Test
    void highestBottleneckAmongShortest() {
        // A square: two paths of equal length from one corner to the other; the lower one has a slow duct.
        Set<NodePos> nodes = new HashSet<>(List.of(p(0, 0), p(1, 0), p(2, 0), p(0, 1), p(2, 1), p(0, 2), p(1, 2), p(2, 2)));
        Map<NodePos, Double> bandwidth = new HashMap<>();
        nodes.forEach(node -> bandwidth.put(node, 128.0));
        bandwidth.put(p(1, 0), 32.0);
        List<NodePos> path = PathPick.best(Set.of(p(0, 0)), Set.of(p(2, 2)), at -> neighbours(nodes, at),
                bandwidth::get).orElseThrow();
        assertEquals(5, path.size());
        assertTrue(!path.contains(p(1, 0)), "the path avoids the slow duct, got " + path);
        assertEquals(128.0, PathPick.bottleneck(path, bandwidth::get), 1e-9);
        // Swapped, the other side is slow and the first path wins.
        bandwidth.put(p(1, 0), 128.0);
        bandwidth.put(p(0, 1), 32.0);
        List<NodePos> other = PathPick.best(Set.of(p(0, 0)), Set.of(p(2, 2)), at -> neighbours(nodes, at),
                bandwidth::get).orElseThrow();
        assertTrue(!other.contains(p(0, 1)), "the path avoids the slow duct, got " + other);
    }
}
