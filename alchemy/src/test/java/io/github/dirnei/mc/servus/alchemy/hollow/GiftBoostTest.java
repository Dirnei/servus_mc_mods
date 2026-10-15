package io.github.dirnei.mc.servus.alchemy.hollow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GiftBoostTest {

    private static int workFor(GiftBoost boost, int ticks) {
        int work = 0;
        for (int i = 0; i < ticks; i++) {
            work += boost.work();
        }
        return work;
    }

    @Test
    void baseRateWithoutGift() {
        GiftBoost boost = new GiftBoost();
        assertFalse(boost.active());
        assertEquals(1, boost.factor());
        assertEquals(10, workFor(boost, 10));
    }

    @Test
    void factorTwoDoublesWork() {
        GiftBoost boost = new GiftBoost();
        boost.give(2, 100);
        assertTrue(boost.active());
        assertEquals(20, workFor(boost, 10));
        assertEquals(90, boost.ticksLeft());
    }

    @Test
    void fractionalFactorCarries() {
        GiftBoost boost = new GiftBoost();
        boost.give(1.5, 100);
        assertEquals(15, workFor(boost, 10));
    }

    @Test
    void runsOutAfterDuration() {
        GiftBoost boost = new GiftBoost();
        boost.give(2, 5);
        assertEquals(10, workFor(boost, 5));
        assertFalse(boost.active());
        assertEquals(0, boost.ticksLeft());
        assertEquals(5, workFor(boost, 5), "base rate again once the boost ran out");
    }

    @Test
    void newGiftRefreshesInsteadOfStacking() {
        GiftBoost boost = new GiftBoost();
        boost.give(2, 100);
        workFor(boost, 90);
        boost.give(2, 100);
        assertEquals(100, boost.ticksLeft());
        assertEquals(2, boost.factor());
    }

    @Test
    void loadKeepsFactorAndTicks() {
        GiftBoost boost = new GiftBoost();
        boost.load(3, 42);
        assertEquals(3, boost.factor());
        assertEquals(42, boost.ticksLeft());
    }
}
