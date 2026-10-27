package io.github.dirnei.mc.servus.shuttles.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FluidPutAwayPlanTest {

    @Test
    void belowMinLoadWaits() {
        assertEquals(0, FluidPutAwayPlan.amount(300, false, 1000, 4000, 16000));
        assertEquals(1000, FluidPutAwayPlan.amount(1000, false, 1000, 4000, 16000));
    }

    @Test
    void fullTankGoesBelowMinLoad() {
        assertEquals(500, FluidPutAwayPlan.amount(500, true, 1000, 4000, 16000));
    }

    @Test
    void limitedByShuttleCapacity() {
        assertEquals(4000, FluidPutAwayPlan.amount(12000, false, 1000, 4000, 16000));
    }

    @Test
    void limitedByRoom() {
        assertEquals(1500, FluidPutAwayPlan.amount(12000, false, 1000, 4000, 1500));
    }

    @Test
    void noRoomIsZero() {
        assertEquals(0, FluidPutAwayPlan.amount(12000, true, 1000, 4000, 0));
        assertEquals(0, FluidPutAwayPlan.amount(0, true, 1000, 4000, 4000));
    }
}
