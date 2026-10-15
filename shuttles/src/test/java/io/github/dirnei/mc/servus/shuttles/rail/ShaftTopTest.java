package io.github.dirnei.mc.servus.shuttles.rail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ShaftTopTest {

    @Test
    void firstFreeAboveColumn() {
        // Segments at heights 1 to 4.
        assertEquals(OptionalInt.of(5), ShaftTop.firstFree(1, 320, y -> y >= 1 && y <= 4));
        assertEquals(OptionalInt.of(5), ShaftTop.firstFree(3, 320, y -> y >= 1 && y <= 4));
    }

    @Test
    void heightLimitStops() {
        assertTrue(ShaftTop.firstFree(1, 5, y -> y >= 1 && y <= 4).isEmpty());
        assertEquals(OptionalInt.of(5), ShaftTop.firstFree(1, 6, y -> y >= 1 && y <= 4));
    }

    @Test
    void noSegmentNoWalk() {
        assertTrue(ShaftTop.firstFree(0, 320, y -> y >= 1).isEmpty());
    }
}
