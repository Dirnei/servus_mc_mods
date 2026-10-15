package io.github.dirnei.mc.servus.alchemy.hollow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.alchemy.Principles;
import org.junit.jupiter.api.Test;

class PrincipleDripTest {

    private static final Principles IRON_STEP = new Principles(100, 0, 20);
    private static final Principles PLENTY = new Principles(4000, 4000, 4000);

    @Test
    void dripsOnlyWhatTheStepTakes() {
        Principles drip = PrincipleDrip.plan(new Principles(500, 500, 500), IRON_STEP, PLENTY, 25);
        assertEquals(new Principles(25, 0, 25), drip);
    }

    @Test
    void limitedByPerDrip() {
        Principles drip = PrincipleDrip.plan(new Principles(500, 0, 500), IRON_STEP, PLENTY, 100);
        assertEquals(new Principles(100, 0, 100), drip);
    }

    @Test
    void limitedByHeldAndRoom() {
        Principles drip = PrincipleDrip.plan(new Principles(10, 0, 500), IRON_STEP, new Principles(4000, 4000, 5), 25);
        assertEquals(new Principles(10, 0, 5), drip);
    }

    @Test
    void nothingWithoutCost() {
        assertEquals(Principles.NONE, PrincipleDrip.plan(PLENTY, Principles.NONE, PLENTY, 25));
    }

    @Test
    void nothingWhenEmptyOrFull() {
        assertEquals(Principles.NONE, PrincipleDrip.plan(Principles.NONE, IRON_STEP, PLENTY, 25));
        assertEquals(Principles.NONE, PrincipleDrip.plan(PLENTY, IRON_STEP, Principles.NONE, 25));
    }
}
