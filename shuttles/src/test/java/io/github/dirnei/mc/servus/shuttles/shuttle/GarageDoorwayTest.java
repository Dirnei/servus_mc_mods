package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class GarageDoorwayTest {

    private static final NodePos INSIDE = n(0, 0);
    private static final NodePos HOME = n(0, 1);

    private static NodePos n(int x, int z) {
        return new NodePos(x, 0, z);
    }

    /** Rails from (-2, 1) to (2, 1) behind the garage at (0, 0), and a rail at (0, -1) in front of it, linked to nothing. */
    private static Function<NodePos, Set<NodePos>> rails() {
        Map<NodePos, Set<NodePos>> edges = new HashMap<>();
        for (int x = -2; x < 2; x++) {
            edges.computeIfAbsent(n(x, 1), k -> new HashSet<>()).add(n(x + 1, 1));
            edges.computeIfAbsent(n(x + 1, 1), k -> new HashSet<>()).add(n(x, 1));
        }
        return node -> edges.getOrDefault(node, Set.of());
    }

    @Test
    void insideLeadsOnlyToTheHomeRail() {
        Function<NodePos, Set<NodePos>> ways = GarageDoorway.through(rails(), INSIDE, HOME);
        assertEquals(Set.of(HOME), ways.apply(INSIDE));
    }

    @Test
    void homeRailLeadsInsideBesideItsRails() {
        Function<NodePos, Set<NodePos>> ways = GarageDoorway.through(rails(), INSIDE, HOME);
        assertEquals(Set.of(n(-1, 1), n(1, 1), INSIDE), ways.apply(HOME));
    }

    @Test
    void otherRailsKeepTheirSteps() {
        Function<NodePos, Set<NodePos>> ways = GarageDoorway.through(rails(), INSIDE, HOME);
        assertEquals(Set.of(n(0, 1), n(2, 1)), ways.apply(n(1, 1)));
    }

    @Test
    void pathOutStartsInsideAndPassesTheHomeRail() {
        Function<NodePos, Set<NodePos>> ways = GarageDoorway.through(rails(), INSIDE, HOME);
        List<NodePos> path = RailPaths.find(INSIDE, n(2, 1)::equals, ways);
        assertEquals(List.of(INSIDE, HOME, n(1, 1), n(2, 1)), path);
    }

    @Test
    void pathHomeEndsInside() {
        Function<NodePos, Set<NodePos>> ways = GarageDoorway.through(rails(), INSIDE, HOME);
        List<NodePos> path = RailPaths.find(n(-2, 1), INSIDE::equals, ways);
        assertEquals(List.of(n(-2, 1), n(-1, 1), HOME, INSIDE), path);
    }

    @Test
    void theGarageIsNoWayToTheRailInFrontOfIt() {
        Function<NodePos, Set<NodePos>> ways = GarageDoorway.through(rails(), INSIDE, HOME);
        assertTrue(RailPaths.find(n(2, 1), n(0, -1)::equals, ways).isEmpty());
    }
}
