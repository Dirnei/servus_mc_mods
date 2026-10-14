package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FieldWaterTest {

    @Test
    void perRail() {
        assertEquals(30, FieldWater.use(10, 3));
    }

    @Test
    void noRailsNoUse() {
        assertEquals(0, FieldWater.use(0, 5));
    }

    @Test
    void atLeastOne() {
        assertEquals(1, FieldWater.use(3, 0));
    }

    @Test
    void noOverflow() {
        assertEquals(Integer.MAX_VALUE, FieldWater.use(Integer.MAX_VALUE, 4));
    }

    @Test
    void perMinuteOfFiveRails() {
        assertEquals(60, FieldWater.perMinute(5, 1, 100));
        assertEquals(0, FieldWater.perMinute(0, 1, 100));
    }

    @Test
    void ticksToUseWholeIntervals() {
        assertEquals(100, FieldWater.ticksToUse(5, 5, 1, 100));
        assertEquals(200, FieldWater.ticksToUse(6, 5, 1, 100));
        assertEquals(64000 / 5 * 100, FieldWater.ticksToUse(64000, 5, 1, 100));
        assertEquals(-1, FieldWater.ticksToUse(10, 0, 1, 100));
    }
}
