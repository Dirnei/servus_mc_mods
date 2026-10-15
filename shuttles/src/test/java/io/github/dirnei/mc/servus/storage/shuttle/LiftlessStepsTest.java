package io.github.dirnei.mc.servus.storage.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LiftlessStepsTest {

    private static final NodePos SHAFT = new NodePos(0, 64, 0);

    @Test
    void stepsUpAndDownAreDropped() {
        Set<NodePos> kept = LiftlessSteps.filter(SHAFT, Set.of(new NodePos(0, 65, 0), new NodePos(0, 63, 0)));
        assertEquals(Set.of(), kept);
    }

    @Test
    void crossingTheShaftOnItsFloorStays() {
        Set<NodePos> sides = Set.of(new NodePos(-1, 64, 0), new NodePos(1, 64, 0));
        assertEquals(sides, LiftlessSteps.filter(SHAFT, sides));
    }

    @Test
    void mixedNeighboursKeepOnlyTheFloor() {
        Set<NodePos> kept = LiftlessSteps.filter(SHAFT, Set.of(new NodePos(0, 65, 0), new NodePos(0, 64, 1)));
        assertEquals(Set.of(new NodePos(0, 64, 1)), kept);
    }
}
