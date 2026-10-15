package io.github.dirnei.mc.servus.alchemy.homunculus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.homunculus.HomunculusLife.Step;
import org.junit.jupiter.api.Test;

class HomunculusLifeTest {

    private static final HomunculusSettings SETTINGS = new HomunculusSettings(5, 3, 100, 50, 2);

    @Test
    void growsOnlyWhenFed() {
        HomunculusLife life = new HomunculusLife();
        assertEquals(Step.NONE, life.tick(SETTINGS, false));
        assertEquals(0, life.growth());
        assertEquals(Step.GREW, life.tick(SETTINGS, true));
        assertEquals(1, life.growth());
        assertEquals(0.2F, life.growthShare(SETTINGS), 1e-6);
    }

    @Test
    void growsUpOnce() {
        HomunculusLife life = new HomunculusLife();
        for (int i = 0; i < 4; i++) {
            assertEquals(Step.GREW, life.tick(SETTINGS, true));
        }
        assertEquals(Step.GREW_UP, life.tick(SETTINGS, true));
        assertTrue(life.grown());
        assertEquals(1F, life.growthShare(SETTINGS));
        assertEquals(Step.NONE, life.tick(SETTINGS, true));
    }

    @Test
    void worksEveryIntervalWhenGrownAndFed() {
        HomunculusLife life = new HomunculusLife();
        life.load(5, true, false, 0);
        int work = 0;
        for (int i = 0; i < 9; i++) {
            if (life.tick(SETTINGS, true) == Step.WORK) {
                work++;
            }
        }
        assertEquals(3, work);
        assertEquals(Step.NONE, life.tick(SETTINGS, false), "no Elixir, no work");
        assertEquals(0, life.workTimer(), "the timer waits while it starves");
    }

    @Test
    void freedWorksUnfedAndNeedsNoElixir() {
        HomunculusLife life = new HomunculusLife();
        assertFalse(life.free(), "a growing homunculus cannot be freed");
        life.load(5, true, false, 0);
        assertTrue(life.free());
        assertFalse(life.free(), "freed only once");
        assertFalse(life.needsElixir());
        life.tick(SETTINGS, false);
        life.tick(SETTINGS, false);
        assertEquals(Step.WORK, life.tick(SETTINGS, false));
    }

    @Test
    void savedStateResumes() {
        HomunculusLife life = new HomunculusLife();
        life.load(4, false, true, 7);
        assertFalse(life.freed(), "only a grown homunculus is freed");
        assertEquals(Step.GREW_UP, life.tick(SETTINGS, true));
    }

    @Test
    void giftWhenBoostRunsLow() {
        assertTrue(HomunculusLife.wantsGift(0, SETTINGS));
        assertTrue(HomunculusLife.wantsGift(49, SETTINGS));
        assertFalse(HomunculusLife.wantsGift(50, SETTINGS));
    }
}
