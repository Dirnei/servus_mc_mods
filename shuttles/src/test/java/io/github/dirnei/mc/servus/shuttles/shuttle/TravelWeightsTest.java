package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.shuttles.shuttle.Shafts.Shaft;
import org.junit.jupiter.api.Test;

class TravelWeightsTest {

    private static final TravelWeights WEIGHTS = new TravelWeights(7, 5, 10, 20, 14, 50);
    private static final Shaft SHAFT = new Shaft(new NodePos(0, 0, 0), 3);

    /** A shaft at x 0, z 0 whose car stands at y 3 with one call queued. */
    private static final TravelWeights.Lifts LIFTS = new TravelWeights.Lifts() {
        @Override
        public Shaft shaftOf(NodePos node) {
            return SHAFT.contains(node) ? SHAFT : null;
        }

        @Override
        public double waitAt(Shaft shaft, int floorY) {
            return WEIGHTS.liftWait(3, floorY, 1);
        }
    };

    @Test
    void railBlockAndCorner() {
        NodePos a = new NodePos(0, 5, 0);
        NodePos b = new NodePos(1, 5, 0);
        assertEquals(7, WEIGHTS.step(null, a, b, LIFTS), 1e-9);
        assertEquals(7, WEIGHTS.step(a, b, new NodePos(2, 5, 0), LIFTS), 1e-9);
        assertEquals(12, WEIGHTS.step(a, b, new NodePos(1, 5, 1), LIFTS), 1e-9);
    }

    @Test
    void rideEntryPaysHandoverAndWait() {
        NodePos rail = new NodePos(1, 0, 0);
        NodePos bottom = new NodePos(0, 0, 0);
        NodePos up = new NodePos(0, 1, 0);
        double entry = WEIGHTS.step(rail, bottom, up, LIFTS);
        assertEquals(10 + 20 + 3 * 10 + 50, entry, 1e-9);
        assertEquals(10, WEIGHTS.step(bottom, up, new NodePos(0, 2, 0), LIFTS), 1e-9);
    }

    @Test
    void leavingARideIsNoCorner() {
        NodePos below = new NodePos(0, 1, 0);
        NodePos top = new NodePos(0, 2, 0);
        assertEquals(7, WEIGHTS.step(below, top, new NodePos(1, 2, 0), LIFTS), 1e-9);
    }

    @Test
    void flatWayBeatsASlowLift() {
        double flat = 10 * WEIGHTS.railBlock();
        double lift = 3 * WEIGHTS.railBlock() + WEIGHTS.liftBlock() + WEIGHTS.handover() + WEIGHTS.liftWait(3, 0, 0);
        assertTrue(flat < lift, flat + " should be below " + lift);
    }
}
