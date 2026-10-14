package io.github.dirnei.mc.servus.alchemy.hollow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinGrowth.Outcome;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinGrowth.State;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinGrowth.Vein;
import org.junit.jupiter.api.Test;

class VeinGrowthTest {

    private static final Vein IRON = new Vein(2, 3, new Principles(100, 0, 20), 4);
    private static final Principles PLENTY = new Principles(1000, 0, 1000);

    private static Outcome run(State state, Principles held, boolean room, int ticks) {
        Outcome last = Outcome.NONE;
        for (int i = 0; i < ticks; i++) {
            Outcome outcome = VeinGrowth.tick(IRON, state, held, room);
            if (outcome != Outcome.NONE) {
                last = outcome;
            }
        }
        return last;
    }

    @Test
    void waitsWithoutPrinciples() {
        State state = new State();
        assertEquals(Outcome.NONE, run(state, new Principles(50, 0, 20), true, 10));
        assertEquals(0, state.ripeness);
        assertEquals(0, state.stepTimer, "the timer does not run while waiting");
    }

    @Test
    void stepsFollowTheStepTime() {
        State state = new State();
        assertEquals(Outcome.NONE, VeinGrowth.tick(IRON, state, PLENTY, true));
        assertEquals(Outcome.NONE, VeinGrowth.tick(IRON, state, PLENTY, true));
        assertEquals(Outcome.RIPENED, VeinGrowth.tick(IRON, state, PLENTY, true));
        assertEquals(1, state.ripeness);
    }

    @Test
    void ripenessStopsAtTheSteps() {
        State state = new State();
        run(state, PLENTY, true, 30);
        assertEquals(2, state.ripeness);
        assertTrue(state.ripe(IRON));
    }

    @Test
    void noYieldBeforeABinding() {
        State state = new State();
        state.ripeness = 2;
        assertEquals(Outcome.NONE, run(state, PLENTY, true, 20));
        state.bound = true;
        assertEquals(Outcome.YIELD, run(state, Principles.NONE, true, 4), "a bound gnome needs no principles");
    }

    @Test
    void yieldRateIsFixed() {
        State state = new State();
        state.ripeness = 2;
        state.bound = true;
        int yields = 0;
        for (int i = 0; i < 40; i++) {
            if (VeinGrowth.tick(IRON, state, Principles.NONE, true) == Outcome.YIELD) {
                yields++;
            }
        }
        assertEquals(10, yields);
    }

    @Test
    void fullOutputStops() {
        State state = new State();
        state.ripeness = 2;
        state.bound = true;
        assertEquals(Outcome.NONE, run(state, Principles.NONE, false, 20));
        assertFalse(state.yieldTimer > 0, "the gnome rests while the output is full");
    }

    @Test
    void zeroNumbersAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Vein(0, 1, Principles.NONE, 1));
    }
}
