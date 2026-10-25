package io.github.dirnei.mc.servus.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CrystalRamp;
import org.junit.jupiter.api.Test;

class CrystalRampTest {

    private static final CrystalRamp RAMP = CrystalRamp.of(0x000000, 0x808080, 0xFFFFFF);

    @Test
    void endsGiveTheFirstAndLastStop() {
        assertEquals(0x000000, RAMP.at(CrystalRamp.DARKEST));
        assertEquals(0xFFFFFF, RAMP.at(CrystalRamp.BRIGHTEST));
    }

    @Test
    void brightnessOutsideTheRampIsClamped() {
        assertEquals(0x000000, RAMP.at(0));
        assertEquals(0xFFFFFF, RAMP.at(3));
    }

    @Test
    void middleOfTheRampIsTheMiddleStop() {
        assertEquals(0x808080, RAMP.at((CrystalRamp.DARKEST + CrystalRamp.BRIGHTEST) / 2));
    }

    @Test
    void mixingGoesStopByStop() {
        CrystalRamp red = CrystalRamp.of(0xFF0000, 0xFF0000, 0xFF0000);
        assertEquals(RAMP, RAMP.mix(red, 0));
        assertEquals(red, RAMP.mix(red, 1));
        CrystalRamp half = RAMP.mix(red, 0.5F);
        assertEquals(0x800000, half.stop(0));
        assertEquals(0xC04040, half.stop(1));
        assertEquals(0xFF8080, half.stop(2));
    }

    @Test
    void towardWhiteBrightensEveryStop() {
        assertEquals(CrystalRamp.of(0xFFFFFF, 0xFFFFFF, 0xFFFFFF), RAMP.toward(0xFFFFFF, 1));
    }
}
