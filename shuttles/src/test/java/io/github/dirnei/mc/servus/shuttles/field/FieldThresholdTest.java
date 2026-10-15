package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FieldThresholdTest {

    @Test
    void shareRoundsToAStep() {
        assertEquals(50, FieldThreshold.fromShare(0.5));
        assertEquals(30, FieldThreshold.fromShare(0.33));
        assertEquals(10, FieldThreshold.fromShare(0.0));
        assertEquals(100, FieldThreshold.fromShare(1.5));
    }

    @Test
    void stepsStayInRange() {
        assertEquals(60, FieldThreshold.step(50, 1));
        assertEquals(40, FieldThreshold.step(50, -1));
        assertEquals(100, FieldThreshold.step(100, 1));
        assertEquals(10, FieldThreshold.step(10, -1));
    }

    @Test
    void sliderFractionRoundsToStep() {
        assertEquals(10, FieldThreshold.fromFraction(0.0));
        assertEquals(50, FieldThreshold.fromFraction(0.47));
        assertEquals(60, FieldThreshold.fromFraction(0.55));
        assertEquals(100, FieldThreshold.fromFraction(1.2));
    }
}
