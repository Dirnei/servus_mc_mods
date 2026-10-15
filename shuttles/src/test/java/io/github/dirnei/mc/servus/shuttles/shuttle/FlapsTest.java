package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FlapsTest {

    @Test
    void upAtTheFloor() {
        assertEquals(90, Flaps.angle(4, 4), 1e-9);
    }

    @Test
    void upWithinHalfABlock() {
        assertEquals(90, Flaps.angle(4.5, 4), 1e-9);
        assertEquals(90, Flaps.angle(3.5, 4), 1e-9);
    }

    @Test
    void halfWayBetween() {
        assertEquals(45, Flaps.angle(4.75, 4), 1e-9);
        assertEquals(45, Flaps.angle(3.25, 4), 1e-9);
    }

    @Test
    void downOneFloorAway() {
        assertEquals(0, Flaps.angle(5, 4), 1e-9);
        assertEquals(0, Flaps.angle(3, 4), 1e-9);
    }

    @Test
    void downFarAway() {
        assertEquals(0, Flaps.angle(7, 4), 1e-9);
        assertEquals(0, Flaps.angle(1, 4), 1e-9);
    }
}
