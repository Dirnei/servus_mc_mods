package io.github.dirnei.mc.servus.lib.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NetworkGraphAddTest {

    private static NodePos x(int x) {
        return new NodePos(x, 0, 0);
    }

    @Test
    void isolatedNodeCreatesComponent() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());

        GraphChange change = graph.addNode(x(0), "a");

        assertEquals(List.of(change.survivor()), change.created());
        assertTrue(change.merged().isEmpty());
        assertTrue(change.removed().isEmpty());
        assertEquals(Set.of(x(0)), graph.members(change.survivor()));
    }

    @Test
    void extendingChainKeepsId() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        int id = graph.addNode(x(0), "a").survivor();

        GraphChange change = graph.addNode(x(1), "b");

        assertEquals(id, change.survivor());
        assertTrue(change.isEmpty());
        assertEquals(id, graph.componentOf(x(1)).getAsInt());
        assertEquals(Set.of(x(0), x(1)), graph.members(id));
    }

    @Test
    void bridgingMergesIntoLargest() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        int big = graph.addNode(x(0), "a").survivor();
        graph.addNode(x(1), "b");
        graph.addNode(x(2), "c");
        int small = graph.addNode(x(4), "e").survivor();
        assertNotEquals(big, small);

        GraphChange change = graph.addNode(x(3), "d");

        assertEquals(big, change.survivor());
        assertEquals(List.of(small), change.merged());
        assertTrue(change.created().isEmpty());
        assertEquals(Set.of(x(0), x(1), x(2), x(3), x(4)), graph.members(big));
        assertTrue(graph.members(small).isEmpty());
        assertEquals(Set.of(big), graph.componentIds());
    }

    @Test
    void mergingThreeComponents() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        NodePos center = new NodePos(0, 0, 0);
        int west = graph.addNode(center.relative(Side.WEST), "w").survivor();
        int east = graph.addNode(center.relative(Side.EAST), "e").survivor();
        int up = graph.addNode(center.relative(Side.UP), "u").survivor();

        GraphChange change = graph.addNode(center, "c");

        assertEquals(2, change.merged().size());
        assertEquals(1, graph.componentIds().size());
        assertEquals(4, graph.members(change.survivor()).size());
        assertTrue(Set.of(west, east, up).contains(change.survivor()));
    }

    @Test
    void asymmetricRuleYieldsNoEdge() {
        ConnectionRule<String> rule = (from, fromNode, side, to, toNode) -> !fromNode.equals("blocker");
        NetworkGraph<String> graph = new NetworkGraph<>(rule);
        int a = graph.addNode(x(0), "open").survivor();

        int b = graph.addNode(x(1), "blocker").survivor();

        assertNotEquals(a, b);
        assertFalse(graph.neighbors(x(0)).contains(x(1)));
        assertFalse(graph.neighbors(x(1)).contains(x(0)));
    }

    @Test
    void symmetricRuleYieldsEdgeBothWays() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        graph.addNode(x(0), "a");
        graph.addNode(x(1), "b");

        assertEquals(Set.of(x(1)), graph.neighbors(x(0)));
        assertEquals(Set.of(x(0)), graph.neighbors(x(1)));
    }

    @Test
    void duplicateAddThrows() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        graph.addNode(x(0), "a");

        assertThrows(IllegalArgumentException.class, () -> graph.addNode(x(0), "b"));
        assertEquals(1, graph.size());
    }
}
