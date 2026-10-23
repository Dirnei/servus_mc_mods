package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.shuttles.shuttle.Shafts.Shaft;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class RailCostsTest {

    private static final TravelWeights WEIGHTS = new TravelWeights(7, 5, 10, 20, 14, 50);

    /** A graph of the given nodes, linking every pair one block apart. */
    private static Function<NodePos, Set<NodePos>> grid(Set<NodePos> nodes) {
        return node -> {
            Set<NodePos> next = new HashSet<>();
            int[][] steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            for (int[] step : steps) {
                NodePos candidate = new NodePos(node.x() + step[0], node.y() + step[1], node.z() + step[2]);
                if (nodes.contains(candidate)) {
                    next.add(candidate);
                }
            }
            return next;
        };
    }

    private static RailCosts.StepCost<NodePos> flat() {
        return (previous, from, to) -> WEIGHTS.step(previous, from, to, TravelWeights.NO_WAIT);
    }

    private static void line(Set<NodePos> nodes, int x0, int z0, int x1, int z1, int y) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                nodes.add(new NodePos(x, y, z));
            }
        }
    }

    @Test
    void staysOnTheStraightLine() {
        Set<NodePos> nodes = new HashSet<>();
        line(nodes, 0, 0, 6, 0, 0);
        line(nodes, 0, 0, 0, 2, 0);
        line(nodes, 0, 2, 2, 2, 0);
        line(nodes, 2, 2, 2, 1, 0);
        line(nodes, 2, 1, 4, 1, 0);
        RailCosts.Search<NodePos> search = RailCosts.search(new NodePos(0, 0, 0), grid(nodes), flat());
        List<NodePos> path = search.pathTo(new NodePos(6, 0, 0));
        assertEquals(7, path.size());
        assertEquals(42, search.costTo(new NodePos(6, 0, 0)), 1e-9);
        for (NodePos node : path) {
            assertEquals(0, node.z(), "the straight way stays on z 0");
        }
    }

    @Test
    void cornerAddsItsWeight() {
        Set<NodePos> nodes = new HashSet<>();
        line(nodes, 0, 0, 2, 0, 0);
        line(nodes, 2, 0, 2, 2, 0);
        RailCosts.Search<NodePos> search = RailCosts.search(new NodePos(0, 0, 0), grid(nodes), flat());
        assertEquals(4 * 7 + 5, search.costTo(new NodePos(2, 0, 2)), 1e-9);
    }

    @Test
    void unreachableIsInfinite() {
        Set<NodePos> nodes = Set.of(new NodePos(0, 0, 0), new NodePos(5, 0, 0));
        RailCosts.Search<NodePos> search = RailCosts.search(new NodePos(0, 0, 0), grid(nodes), flat());
        assertTrue(Double.isInfinite(search.costTo(new NodePos(5, 0, 0))));
        assertTrue(search.pathTo(new NodePos(5, 0, 0)).isEmpty());
    }

    @Test
    void multiStartTakesTheNearestStart() {
        Set<NodePos> nodes = new HashSet<>();
        line(nodes, 0, 0, 10, 0, 0);
        RailCosts.Search<NodePos> search = RailCosts.search(List.of(new NodePos(0, 0, 0), new NodePos(9, 0, 0)), grid(nodes), flat());
        assertEquals(7, search.costTo(new NodePos(10, 0, 0)), 1e-9);
        assertEquals(List.of(new NodePos(9, 0, 0), new NodePos(10, 0, 0)), search.pathTo(new NodePos(10, 0, 0)));
    }

    @Test
    void cheapestGoalWins() {
        Set<NodePos> nodes = new HashSet<>();
        line(nodes, 0, 0, 10, 0, 0);
        RailCosts.Search<NodePos> search = RailCosts.search(new NodePos(4, 0, 0), grid(nodes), flat());
        Set<NodePos> goals = Set.of(new NodePos(0, 0, 0), new NodePos(10, 0, 0));
        assertEquals(new NodePos(0, 0, 0), search.cheapest(goals::contains).orElseThrow());
        assertEquals(5, search.pathToCheapest(goals::contains).size());
    }

    @Test
    void busyLiftIsAvoided() {
        Set<NodePos> nodes = new HashSet<>();
        line(nodes, 0, 0, 4, 0, 0);
        line(nodes, 0, 0, 4, 0, 1);
        Shaft west = new Shaft(new NodePos(0, 0, 0), 1);
        Shaft east = new Shaft(new NodePos(4, 0, 0), 1);
        TravelWeights.Lifts lifts = new TravelWeights.Lifts() {
            @Override
            public Shaft shaftOf(NodePos node) {
                return west.contains(node) ? west : east.contains(node) ? east : null;
            }

            @Override
            public double waitAt(Shaft shaft, int floorY) {
                return shaft == west ? WEIGHTS.liftWait(floorY, floorY, 3) : WEIGHTS.liftWait(floorY, floorY, 0);
            }
        };
        Function<NodePos, Set<NodePos>> ways = node -> {
            Set<NodePos> next = new HashSet<>(grid(nodes).apply(node));
            next.removeIf(other -> other.y() != node.y() && shaftOf(lifts, node) == null);
            return next;
        };
        RailCosts.Search<NodePos> search = RailCosts.search(new NodePos(2, 0, 0), ways,
                (previous, from, to) -> WEIGHTS.step(previous, from, to, lifts));
        List<NodePos> path = search.pathTo(new NodePos(2, 1, 0));
        assertTrue(path.contains(new NodePos(4, 1, 0)), "the way goes through the east shaft: " + path);
    }

    private static Shaft shaftOf(TravelWeights.Lifts lifts, NodePos node) {
        return lifts.shaftOf(node);
    }
}
