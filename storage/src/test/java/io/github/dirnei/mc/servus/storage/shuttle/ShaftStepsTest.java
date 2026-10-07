package io.github.dirnei.mc.servus.storage.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.storage.shuttle.ShaftSteps.Kind;
import io.github.dirnei.mc.servus.storage.shuttle.Shafts.Shaft;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShaftStepsTest {

    private static NodePos n(int x, int y, int z) {
        return new NodePos(x, y, z);
    }

    // A shaft at x=1, z=0 from y=0 to y=3.
    private final Map<NodePos, Shaft> shafts = Shafts.floorsOf(Shafts.group(Set.of(n(1, 0, 0), n(1, 1, 0), n(1, 2, 0), n(1, 3, 0))));

    private Kind kind(List<NodePos> path, int i) {
        return ShaftSteps.classify(path, i, shafts::get);
    }

    @Test
    void rideUp() {
        List<NodePos> path = List.of(n(0, 0, 0), n(1, 0, 0), n(1, 1, 0), n(1, 2, 0), n(2, 2, 0), n(3, 2, 0));
        assertEquals(Kind.ENTER_RIDE, kind(path, 0));
        assertEquals(Kind.RIDE, kind(path, 1));
        assertEquals(Kind.RIDE, kind(path, 2));
        assertEquals(Kind.EXIT, kind(path, 3));
        assertEquals(Kind.PLAIN, kind(path, 4));
    }

    @Test
    void straightCrossing() {
        List<NodePos> path = List.of(n(0, 1, 0), n(1, 1, 0), n(2, 1, 0));
        assertEquals(Kind.ENTER_CROSS, kind(path, 0));
        assertEquals(Kind.EXIT, kind(path, 1));
    }

    @Test
    void pathEndingOnASegmentRides() {
        List<NodePos> path = List.of(n(-1, 0, 0), n(0, 0, 0), n(1, 0, 0));
        assertEquals(Kind.PLAIN, kind(path, 0));
        assertEquals(Kind.ENTER_RIDE, kind(path, 1));
    }

    @Test
    void rideEndFindsLastNodeOfTheRun() {
        List<NodePos> path = List.of(n(0, 0, 0), n(1, 0, 0), n(1, 1, 0), n(1, 2, 0), n(2, 2, 0));
        assertEquals(3, ShaftSteps.rideEnd(path, 1));
        assertEquals(4, ShaftSteps.rideEnd(path, 4));
        List<NodePos> down = List.of(n(1, 3, 0), n(1, 2, 0), n(1, 1, 0));
        assertEquals(2, ShaftSteps.rideEnd(down, 0));
    }
}
