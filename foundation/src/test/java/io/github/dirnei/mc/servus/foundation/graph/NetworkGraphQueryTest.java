package io.github.dirnei.mc.servus.foundation.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NetworkGraphQueryTest {

    private static NodePos x(int x) {
        return new NodePos(x, 0, 0);
    }

    @Test
    void membershipAfterMerge() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        graph.addNode(x(0), "a");
        graph.addNode(x(1), "b");
        graph.addNode(x(3), "d");
        int survivor = graph.addNode(x(2), "c").survivor();

        for (int i = 0; i <= 3; i++) {
            assertEquals(survivor, graph.componentOf(x(i)).getAsInt());
            assertTrue(graph.members(survivor).contains(x(i)));
        }
        assertEquals(4, graph.members(survivor).size());
    }

    @Test
    void unknownPositionHasNothing() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        graph.addNode(x(0), "a");

        assertFalse(graph.contains(x(7)));
        assertTrue(graph.node(x(7)).isEmpty());
        assertTrue(graph.componentOf(x(7)).isEmpty());
        assertTrue(graph.neighbors(x(7)).isEmpty());
        assertTrue(graph.members(999).isEmpty());
    }

    @Test
    void nodeValueIsReturned() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        graph.addNode(x(0), "payload");

        assertEquals("payload", graph.node(x(0)).orElseThrow());
        assertTrue(graph.contains(x(0)));
    }

    @Test
    void membersViewIsReadOnly() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        int id = graph.addNode(x(0), "a").survivor();

        assertThrows(UnsupportedOperationException.class, () -> graph.members(id).add(x(9)));
        assertThrows(UnsupportedOperationException.class, () -> graph.neighbors(x(0)).add(x(9)));
    }

    @Test
    void refreshAfterRuleChangeSplitsChain() {
        Set<NodePos> closed = new HashSet<>();
        ConnectionRule<String> rule = (from, fromNode, side, to, toNode) -> !closed.contains(from);
        NetworkGraph<String> graph = new NetworkGraph<>(rule);
        int id = graph.addNode(x(0), "a").survivor();
        graph.addNode(x(1), "b");
        graph.addNode(x(2), "c");
        graph.addNode(x(3), "d");

        closed.add(x(1));
        GraphChange change = graph.refresh(x(1));

        // x(1) no longer connects to anything: {0}, {1}, {2,3}
        assertEquals(3, graph.componentIds().size());
        assertEquals(id, graph.componentOf(x(2)).getAsInt());
        assertEquals(id, graph.componentOf(x(3)).getAsInt());
        assertEquals(change.survivor(), graph.componentOf(x(1)).getAsInt());
        assertEquals(2, change.created().size());
        assertTrue(change.merged().isEmpty());
        assertTrue(change.removed().isEmpty());
    }

    @Test
    void refreshWithoutChangeReportsNothing() {
        NetworkGraph<String> graph = new NetworkGraph<>(ConnectionRule.always());
        int id = graph.addNode(x(0), "a").survivor();
        graph.addNode(x(1), "b");
        graph.addNode(x(2), "c");

        GraphChange change = graph.refresh(x(1));

        assertEquals(id, change.survivor());
        assertTrue(change.isEmpty());
        assertEquals(Set.of(id), graph.componentIds());
    }
}
