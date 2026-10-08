package io.github.dirnei.mc.servus.foundation.network.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.ConnectionRule;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetworkSetTest {

    private static NodePos x(int x) {
        return new NodePos(x, 0, 0);
    }

    private NetworkSet<String, String, TestNetwork> set;

    @BeforeEach
    void setUp() {
        TestNetwork.LOG.clear();
        set = new NetworkSet<>(ConnectionRule.always(), TestNetwork::new);
    }

    @Test
    void firstNodeCreatesNetwork() {
        TestNetwork network = set.nodeAdded(x(0), "a");

        assertEquals(1, set.networks().size());
        assertSame(network, set.networkAt(x(0)).orElseThrow());
        assertEquals(Set.of(x(0)), network.members());
        assertEquals(List.of("create " + network.id(), "added " + network.id() + " a"), TestNetwork.LOG);
    }

    @Test
    void neighbourJoinsExistingNetwork() {
        TestNetwork first = set.nodeAdded(x(0), "a");

        TestNetwork second = set.nodeAdded(x(1), "b");

        assertSame(first, second);
        assertEquals(1, set.networks().size());
        assertEquals(2, first.size());
    }

    @Test
    void bridgingMergesAndAbsorbsBeforeDispose() {
        TestNetwork big = set.nodeAdded(x(0), "a");
        set.nodeAdded(x(1), "b");
        set.nodeAdded(x(2), "c");
        TestNetwork small = set.nodeAdded(x(4), "e");
        TestNetwork.LOG.clear();

        TestNetwork survivor = set.nodeAdded(x(3), "d");

        assertSame(big, survivor);
        assertTrue(small.isDisposed());
        assertFalse(big.isDisposed());
        assertEquals(1, set.networks().size());
        assertEquals(5, big.size());
        assertEquals(List.of(
                "absorb " + big.id() + " <- " + small.id() + " disposed=false",
                "dispose " + small.id(),
                "added " + big.id() + " d"), TestNetwork.LOG);
    }

    @Test
    void cuttingSplitsOffNewNetwork() {
        TestNetwork original = set.nodeAdded(x(0), "a");
        for (int i = 1; i < 6; i++) {
            set.nodeAdded(x(i), "n" + i);
        }
        TestNetwork.LOG.clear();

        // {0,1} and {3,4,5}
        set.nodeRemoved(x(2));

        assertEquals(2, set.networks().size());
        TestNetwork large = set.networkAt(x(4)).orElseThrow();
        TestNetwork smallPart = set.networkAt(x(0)).orElseThrow();
        assertSame(original, large);
        assertNotSame(original, smallPart);
        assertEquals(3, large.size());
        assertEquals(2, smallPart.size());
        assertEquals("removed " + original.id() + " n2 size=6", TestNetwork.LOG.get(0));
        assertEquals("create " + smallPart.id(), TestNetwork.LOG.get(1));
    }

    @Test
    void splitHandsMovedNodesOver() {
        TestNetwork original = set.nodeAdded(x(0), "a");
        for (int i = 1; i < 6; i++) {
            set.nodeAdded(x(i), "n" + i);
        }
        TestNetwork.LOG.clear();

        set.nodeRemoved(x(2));

        TestNetwork smallPart = set.networkAt(x(0)).orElseThrow();
        assertTrue(TestNetwork.LOG.contains("removed " + original.id() + " a size=3"));
        assertTrue(TestNetwork.LOG.contains("removed " + original.id() + " n1 size=3"));
        assertTrue(TestNetwork.LOG.contains("added " + smallPart.id() + " a"));
        assertTrue(TestNetwork.LOG.contains("added " + smallPart.id() + " n1"));
        assertTrue(TestNetwork.LOG.indexOf("create " + smallPart.id()) < TestNetwork.LOG.indexOf("added " + smallPart.id() + " a"));
        assertTrue(TestNetwork.LOG.stream().noneMatch(line -> line.startsWith("added " + original.id())));
    }

    @Test
    void lastNodeDisposesNetwork() {
        TestNetwork network = set.nodeAdded(x(0), "a");

        set.nodeRemoved(x(0));

        assertTrue(network.isDisposed());
        assertTrue(set.networks().isEmpty());
        assertTrue(set.networkAt(x(0)).isEmpty());
        assertTrue(TestNetwork.LOG.contains("dispose " + network.id()));
    }

    @Test
    void refreshAfterRuleChangeSplits() {
        Set<NodePos> closed = new HashSet<>();
        ConnectionRule<String> rule = (from, fromNode, side, to, toNode) -> !closed.contains(from);
        set = new NetworkSet<>(rule, TestNetwork::new);
        TestNetwork network = set.nodeAdded(x(0), "a");
        set.nodeAdded(x(1), "b");
        set.nodeAdded(x(2), "c");
        set.nodeAdded(x(3), "d");

        closed.add(x(1));
        TestNetwork refreshed = set.nodeRefreshed(x(1));

        assertEquals(3, set.networks().size());
        assertSame(refreshed, set.networkAt(x(1)).orElseThrow());
        assertEquals(1, refreshed.size());
        assertSame(network, set.networkAt(x(2)).orElseThrow());
        assertEquals(2, network.size());
        assertFalse(network.isDisposed());
    }

    @Test
    void disposeAllClearsEverything() {
        TestNetwork a = set.nodeAdded(x(0), "a");
        TestNetwork b = set.nodeAdded(x(5), "b");

        set.disposeAll();

        assertTrue(a.isDisposed());
        assertTrue(b.isDisposed());
        assertTrue(set.networks().isEmpty());
        assertEquals(0, set.graph().size());
    }
}
