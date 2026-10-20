package io.github.dirnei.mc.servus.brewery.brewhouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class KilnClockTest {

    @Test
    void paleModeTakesTheRecipeTime() {
        assertEquals(600, KilnClock.target(600, OptionalInt.of(400), KilnMode.PALE));
        assertFalse(KilnClock.makesDark(KilnMode.PALE, OptionalInt.of(400)));
    }

    @Test
    void darkModeAddsTheDarkTime() {
        assertEquals(1000, KilnClock.target(600, OptionalInt.of(400), KilnMode.DARK));
        assertTrue(KilnClock.makesDark(KilnMode.DARK, OptionalInt.of(400)));
    }

    @Test
    void darkModeWithoutADarkResultIsPale() {
        assertEquals(600, KilnClock.target(600, OptionalInt.empty(), KilnMode.DARK));
        assertFalse(KilnClock.makesDark(KilnMode.DARK, OptionalInt.empty()));
    }

    @Test
    void progressPausesWhileCold() {
        assertEquals(5, KilnClock.step(5, false));
        assertEquals(6, KilnClock.step(5, true));
    }

    @Test
    void doneAtTheTarget() {
        assertFalse(KilnClock.done(599, 600));
        assertTrue(KilnClock.done(600, 600));
    }

    @Test
    void modeToggles() {
        assertEquals(KilnMode.DARK, KilnMode.PALE.next());
        assertEquals(KilnMode.PALE, KilnMode.DARK.next());
    }

    @Test
    void zeroTimeStillTakesATick() {
        assertEquals(1, KilnClock.target(0, OptionalInt.empty(), KilnMode.PALE));
    }
}
