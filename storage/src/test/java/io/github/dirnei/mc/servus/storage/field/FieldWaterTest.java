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
}
