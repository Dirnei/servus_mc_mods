package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ToppleCurveTest {

    @Test
    void startsUpright() {
        assertEquals(0, ToppleCurve.angle(0), 1e-9);
    }

    @Test
    void speedsUpLikeFalling() {
        assertEquals(5.625, ToppleCurve.angle(0.25), 1e-9);
        assertEquals(22.5, ToppleCurve.angle(0.5), 1e-9);
    }

    @Test
    void liesFlatAtTheEnd() {
        assertEquals(90, ToppleCurve.angle(1), 1e-9);
        assertEquals(90, ToppleCurve.angle(1.7), 1e-9);
        assertEquals(0, ToppleCurve.angle(-0.3), 1e-9);
    }
}
