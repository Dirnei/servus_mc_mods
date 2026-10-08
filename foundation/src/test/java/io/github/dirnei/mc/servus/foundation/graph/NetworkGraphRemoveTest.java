package io.github.dirnei.mc.servus.foundation.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NetworkGraphRemoveTest {

    private static NodePos x(int x) {
        return new NodePos(x, 0, 0);
    }

    private static NetworkGraph<String> chain(int length) {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        for (int i = 0; i < length; i++) {
            graph.addNode(x(i), "n" + i);
        }
        return graph;
    }

    @Test
    void removingFromRingKeepsComponent() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        NodePos a = new NodePos(0, 0, 0);
        NodePos b = new NodePos(1, 0, 0);
        NodePos c = new NodePos(1, 0, 1);
        NodePos d = new NodePos(0, 0, 1);
        int id = graph.addNode(a, "a").survivor();
        graph.addNode(b, "b");
        graph.addNode(c, "c");
        graph.addNode(d, "d");

        GraphChange change = graph.removeNode(a);

        assertEquals(id, change.survivor());
        assertTrue(change.isEmpty());
        assertEquals(Set.of(b, c, d), graph.members(id));
        assertEquals(Set.of(id), graph.componentIds());
    }

    @Test
    void removingLeafKeepsComponent() {
        NetworkGraph<String> graph = chain(3);
        int id = graph.componentOf(x(0)).getAsInt();

        GraphChange change = graph.removeNode(x(2));

        assertEquals(id, change.survivor());
        assertTrue(change.isEmpty());
        assertEquals(Set.of(x(0), x(1)), graph.members(id));
    }

    @Test
    void cuttingChainSplitsWithLargestKeepingId() {
        NetworkGraph<String> graph = chain(6);
        int id = graph.componentOf(x(0)).getAsInt();

        // Leaves {0,1} and {3,4,5}
        GraphChange change = graph.removeNode(x(2));

        assertEquals(id, change.survivor());
        assertEquals(1, change.created().size());
        int created = change.created().get(0);
        assertEquals(Set.of(x(3), x(4), x(5)), graph.members(id));
        assertEquals(Set.of(x(0), x(1)), graph.members(created));
        assertEquals(created, graph.componentOf(x(0)).getAsInt());
        assertEquals(Set.of(id, created), graph.componentIds());
    }

    @Test
    void equalPartsStillSplitIntoTwo() {
        NetworkGraph<String> graph = chain(5);
        int id = graph.componentOf(x(0)).getAsInt();

        GraphChange change = graph.removeNode(x(2));

        assertEquals(1, change.created().size());
        assertEquals(2, graph.componentIds().size());
        assertTrue(graph.componentIds().contains(id));
        assertEquals(2, graph.members(id).size());
        assertEquals(2, graph.members(change.created().get(0)).size());
    }

    @Test
    void threeWaySplit() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        NodePos center = new NodePos(0, 0, 0);
        int id = graph.addNode(center, "c").survivor();
        graph.addNode(center.relative(Side.WEST), "w");
        graph.addNode(center.relative(Side.WEST).relative(Side.WEST), "ww");
        graph.addNode(center.relative(Side.EAST), "e");
        graph.addNode(center.relative(Side.UP), "u");

        GraphChange change = graph.removeNode(center);

        assertEquals(id, change.survivor());
        assertEquals(2, change.created().size());
        assertEquals(3, graph.componentIds().size());
        assertEquals(2, graph.members(id).size());
        for (int created : change.created()) {
            assertEquals(1, graph.members(created).size());
        }
    }

    @Test
    void lastNodeRemovesComponent() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        int id = graph.addNode(x(0), "a").survivor();

        GraphChange change = graph.removeNode(x(0));

        assertEquals(GraphChange.NO_COMPONENT, change.survivor());
        assertEquals(List.of(id), change.removed());
        assertFalse(graph.componentIds().contains(id));
        assertTrue(graph.members(id).isEmpty());
        assertEquals(0, graph.size());
    }

    @Test
    void unknownPositionThrows() {
        NetworkGraph<String> graph = chain(1);

        assertThrows(IllegalArgumentException.class, () -> graph.removeNode(x(5)));
        assertEquals(1, graph.size());
    }
}
