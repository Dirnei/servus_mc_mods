package io.github.dirnei.mc.servus.foundation.data.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class PacketRoutesTest {

    private static NodePos p(int x, int z) {
        return new NodePos(x, 0, z);
    }

    /** Neighbours among the given positions, like a graph of touching ducts. */
    private static Function<NodePos, Set<NodePos>> grid(Set<NodePos> nodes) {
        return at -> {
            Set<NodePos> result = new HashSet<>();
            for (Side side : Side.ALL) {
                NodePos next = at.relative(side);
                if (nodes.contains(next)) {
                    result.add(next);
                }
            }
            return result;
        };
    }

    @Test
    void straightLine() {
        Set<NodePos> nodes = Set.of(p(0, 0), p(1, 0), p(2, 0), p(3, 0));
        List<NodePos> path = PacketRoutes.shortest(Set.of(p(0, 0)), Set.of(p(3, 0)), grid(nodes)).orElseThrow();
        assertEquals(List.of(p(0, 0), p(1, 0), p(2, 0), p(3, 0)), path);
    }

    @Test
    void shorterBranch() {
        Set<NodePos> nodes = new HashSet<>();
        for (int x = 0; x <= 3; x++) {
            nodes.add(p(x, 0));
        }
        // A long detour from (0,0) to (3,0) through z = 1..3.
        for (int z = 1; z <= 3; z++) {
            nodes.add(p(0, z));
            nodes.add(p(3, z));
        }
        for (int x = 0; x <= 3; x++) {
            nodes.add(p(x, 3));
        }
        List<NodePos> path = PacketRoutes.shortest(Set.of(p(0, 0)), Set.of(p(3, 0)), grid(nodes)).orElseThrow();
        assertEquals(4, path.size());
    }

    @Test
    void noRoute() {
        Set<NodePos> nodes = Set.of(p(0, 0), p(2, 0));
        assertTrue(PacketRoutes.shortest(Set.of(p(0, 0)), Set.of(p(2, 0)), grid(nodes)).isEmpty());
    }

    @Test
    void startIsGoal() {
        Set<NodePos> nodes = Set.of(p(0, 0));
        assertEquals(List.of(p(0, 0)), PacketRoutes.shortest(Set.of(p(0, 0)), Set.of(p(0, 0)), grid(nodes)).orElseThrow());
    }
}
