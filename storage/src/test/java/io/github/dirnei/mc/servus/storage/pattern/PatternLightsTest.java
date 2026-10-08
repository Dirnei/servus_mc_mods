package io.github.dirnei.mc.servus.storage.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PatternLightsTest {

    @Test
    void onePerPatternForNine() {
        assertEquals(0, PatternLights.lit(0, 9));
        assertEquals(5, PatternLights.lit(5, 9));
        assertEquals(9, PatternLights.lit(9, 9));
    }

    @Test
    void largerCapacityScalesAndRoundsUp() {
        assertEquals(1, PatternLights.lit(1, 18));
        assertEquals(5, PatternLights.lit(9, 18));
        assertEquals(9, PatternLights.lit(18, 18));
    }

    @Test
    void smallerCapacityScales() {
        assertEquals(7, PatternLights.lit(3, 4));
    }

    @Test
    void overfullShowsAllLights() {
        assertEquals(9, PatternLights.lit(20, 18));
    }
}
