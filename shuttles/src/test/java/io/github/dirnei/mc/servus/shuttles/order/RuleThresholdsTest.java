package io.github.dirnei.mc.servus.shuttles.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RuleThresholdsTest {

    @Test
    void capOnly() {
        assertEquals(RuleThresholds.OK, RuleThresholds.check(-1, -1, 4096));
    }

    @Test
    void makeOnly() {
        assertEquals(RuleThresholds.OK, RuleThresholds.check(8, 16, 0));
        assertEquals(RuleThresholds.OK, RuleThresholds.check(0, 1, 0));
    }

    @Test
    void both() {
        assertEquals(RuleThresholds.OK, RuleThresholds.check(8, 16, 1000));
    }

    @Test
    void nothingSet() {
        assertEquals(RuleThresholds.EMPTY, RuleThresholds.check(-1, -1, 0));
    }

    @Test
    void minimumNotBelowMaximum() {
        assertEquals(RuleThresholds.MAKE_ORDER, RuleThresholds.check(16, 16, 0));
        assertEquals(RuleThresholds.MAKE_ORDER, RuleThresholds.check(8, 0, 4096));
        assertEquals(RuleThresholds.MAKE_ORDER, RuleThresholds.check(-1, 16, 0));
    }

    @Test
    void capBelowMaximum() {
        assertEquals(RuleThresholds.CAP_BELOW_MAX, RuleThresholds.check(8, 16, 10));
    }

    @Test
    void exportOnly() {
        assertEquals(RuleThresholds.OK, RuleThresholds.check(-1, -1, 0, 0));
        assertEquals(RuleThresholds.OK, RuleThresholds.check(-1, -1, 0, 2000));
    }

    @Test
    void exportNotAboveFillUpTo() {
        assertEquals(RuleThresholds.EXPORT_NOT_ABOVE, RuleThresholds.check(100, 500, 0, 400));
        assertEquals(RuleThresholds.EXPORT_NOT_ABOVE, RuleThresholds.check(100, 500, 0, 500));
        assertEquals(RuleThresholds.OK, RuleThresholds.check(100, 500, 5000, 2000));
    }

    @Test
    void capEqualToMaximum() {
        assertEquals(RuleThresholds.OK, RuleThresholds.check(8, 16, 16));
    }
}
