package io.github.dirnei.mc.servus.alchemy.homunculus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HomunculusReachTest {

    @Test
    void twentyFourOffsetsWithoutTheCentre() {
        assertEquals(24, HomunculusReach.offsets().size());
        assertFalse(HomunculusReach.offsets().contains(new HomunculusReach.Offset(0, 0)));
    }

    @Test
    void cornersAreInReach() {
        assertTrue(HomunculusReach.inReach(2, 0, 2));
        assertTrue(HomunculusReach.inReach(-2, 0, -2));
        assertTrue(HomunculusReach.offsets().contains(new HomunculusReach.Offset(-2, 2)));
    }

    @Test
    void threeAwayOrAnotherLevelIsOut() {
        assertFalse(HomunculusReach.inReach(3, 0, 0));
        assertFalse(HomunculusReach.inReach(0, 0, -3));
        assertFalse(HomunculusReach.inReach(1, 1, 0));
        assertFalse(HomunculusReach.inReach(1, -1, 0));
        assertFalse(HomunculusReach.inReach(0, 0, 0));
    }
}
