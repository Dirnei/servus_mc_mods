package io.github.dirnei.mc.servus.lib.network.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.lib.graph.ConnectionRule;
import io.github.dirnei.mc.servus.lib.graph.NodePos;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetworkSetTickTest {

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
    void eventsHandledInOrderBeforeTick() {
        TestNetwork network = set.nodeAdded(x(0), "a");
        network.post("first");
        network.post("second");
        TestNetwork.LOG.clear();

        set.tick();

        int id = network.id();
        assertEquals(List.of("handle " + id + " first", "handle " + id + " second", "tick " + id), TestNetwork.LOG);
    }

    @Test
    void selfPostedEventWaitsOneTick() {
        TestNetwork network = set.nodeAdded(x(0), "a");
        network.post("echo");
        TestNetwork.LOG.clear();

        set.tick();
        int id = network.id();
        assertEquals(List.of("handle " + id + " echo", "tick " + id), TestNetwork.LOG);

        TestNetwork.LOG.clear();
        set.tick();
        assertEquals(List.of("handle " + id + " echoed", "tick " + id), TestNetwork.LOG);
    }

    @Test
    void networksTickInAscendingIdOrder() {
        TestNetwork later = set.nodeAdded(x(10), "late");
        TestNetwork earlier = set.nodeAdded(x(0), "early");
        // Ids are allocated in creation order, so "later" has the lower id here.
        TestNetwork.LOG.clear();

        set.tick();

        int low = Math.min(later.id(), earlier.id());
        int high = Math.max(later.id(), earlier.id());
        assertEquals(List.of("tick " + low, "tick " + high), TestNetwork.LOG);
    }

    @Test
    void disposedNetworkDoesNotTick() {
        TestNetwork network = set.nodeAdded(x(0), "a");
        network.post("event");
        set.nodeRemoved(x(0));
        TestNetwork.LOG.clear();

        set.tick();

        assertEquals(List.of(), TestNetwork.LOG);
    }
}
