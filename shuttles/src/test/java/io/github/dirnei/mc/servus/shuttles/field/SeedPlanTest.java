package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SeedPlanTest {

    @Test
    void harvestComesFirst() {
        assertEquals(0, SeedPlan.toFetch(1, 20, 0, 100, 128, 64));
    }

    @Test
    void shortfallOnly() {
        assertEquals(15, SeedPlan.toFetch(0, 20, 5, 100, 128, 64));
        assertEquals(0, SeedPlan.toFetch(0, 20, 25, 100, 128, 64));
    }

    @Test
    void cappedByStackStockAndRoom() {
        assertEquals(64, SeedPlan.toFetch(0, 200, 0, 1000, 128, 64));
        assertEquals(7, SeedPlan.toFetch(0, 200, 0, 7, 128, 64));
        assertEquals(10, SeedPlan.toFetch(0, 200, 0, 1000, 10, 64));
        assertEquals(0, SeedPlan.toFetch(0, 200, 0, 0, 128, 64));
    }

    @Test
    void squaresTakeWholeFours() {
        assertEquals(8, SeedPlan.toFetch(0, 8, 0, 100, 128, 64, SeedPlan.SQUARE));
        assertEquals(4, SeedPlan.toFetch(0, 8, 0, 7, 128, 64, SeedPlan.SQUARE));
        assertEquals(0, SeedPlan.toFetch(0, 8, 0, 3, 128, 64, SeedPlan.SQUARE));
        assertEquals(1, SeedPlan.toFetch(0, 8, 3, 1, 128, 64, SeedPlan.SQUARE), "three carried plus one make a square");
    }
}
