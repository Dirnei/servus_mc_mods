package io.github.dirnei.mc.servus.delicacies.firebox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GaugeNeedleTest {

    @Test
    void carriesTheReadingOnByItsRate() {
        assertEquals(0.35F, GaugeNeedle.shown(0.3F, 0.01F, 5), 1e-6);
        assertEquals(0.25F, GaugeNeedle.shown(0.3F, -0.01F, 5), 1e-6);
        assertEquals(0.3F, GaugeNeedle.shown(0.3F, 0, 500), 1e-6);
    }

    @Test
    void staysBetweenColdAndFull() {
        assertEquals(1, GaugeNeedle.shown(0.9F, 0.1F, 50));
        assertEquals(0, GaugeNeedle.shown(0.1F, -0.1F, 50));
        assertEquals(0.5F, GaugeNeedle.shown(0.5F, 0.1F, -3));
    }

    @Test
    void anglesSweepFromLeftToRight() {
        assertEquals(-120, GaugeNeedle.angle(0), 1e-4);
        assertEquals(0, GaugeNeedle.angle(0.5F), 1e-4);
        assertEquals(120, GaugeNeedle.angle(1), 1e-4);
        assertEquals(120, GaugeNeedle.angle(2), 1e-4);
    }
}
