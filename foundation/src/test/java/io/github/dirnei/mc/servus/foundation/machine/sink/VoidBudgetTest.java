package io.github.dirnei.mc.servus.foundation.machine.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VoidBudgetTest {

    @Test
    void idleBucketGrantsOneSecondOfRate() {
        VoidBudget budget = new VoidBudget();
        assertEquals(1000, budget.take(5000, 100, 50));
    }

    @Test
    void idleForLongStillGrantsOnlyOneSecond() {
        VoidBudget budget = new VoidBudget();
        budget.take(1, 0, 50);
        assertEquals(1000, budget.take(5000, 100, 50));
    }

    @Test
    void secondTakeInTheSameTickGetsTheRemainder() {
        VoidBudget budget = new VoidBudget();
        assertEquals(600, budget.take(600, 10, 50));
        assertEquals(400, budget.take(600, 10, 50));
        assertEquals(0, budget.take(600, 10, 50));
    }

    @Test
    void peekDoesNotSpend() {
        VoidBudget budget = new VoidBudget();
        assertEquals(64, budget.peek(64, 0, 100));
        assertEquals(64, budget.peek(64, 0, 100));
        assertEquals(64, budget.take(64, 0, 100));
        assertEquals(2000 - 64, budget.peek(5000, 0, 100));
    }

    @Test
    void steadyRequestsSettleAtTheRate() {
        VoidBudget budget = new VoidBudget();
        double rate = 0.5;
        // Drain the initial second of carry over.
        for (long tick = 0; tick < 40; tick++) {
            budget.take(1, tick, rate);
        }
        long granted = 0;
        for (long tick = 40; tick < 240; tick++) {
            granted += budget.take(1, tick, rate);
        }
        assertEquals(100, granted);
    }

    @Test
    void fractionalItemRateGrantsOneItemEveryFiveTicks() {
        VoidBudget budget = new VoidBudget();
        double rate = 4 / 20.0;
        assertEquals(4, budget.take(64, 0, rate));
        assertEquals(0, budget.take(64, 4, rate));
        assertEquals(1, budget.take(64, 5, rate));
        long granted = 0;
        for (long tick = 6; tick <= 105; tick++) {
            granted += budget.take(64, tick, rate);
        }
        assertEquals(20, granted);
    }

    @Test
    void unlimitedGrantsEverything() {
        VoidBudget budget = new VoidBudget();
        assertEquals(1_000_000L, budget.take(1_000_000L, 0, SinkRates.UNLIMITED));
        assertEquals(1_000_000L, budget.take(1_000_000L, 0, SinkRates.UNLIMITED));
    }

    @Test
    void rateDropCapsStoredTokens() {
        VoidBudget budget = new VoidBudget();
        budget.take(0, 0, 0);
        budget.take(1, 0, 400);
        // Full at 400 per tick would be 8000; at 100 per tick the cap is 2000.
        assertEquals(2000, budget.take(10_000, 100, 100));
    }

    @Test
    void gameTimeGoingBackwardsCountsAsZeroElapsed() {
        VoidBudget budget = new VoidBudget();
        assertEquals(1000, budget.take(1000, 500, 50));
        assertEquals(0, budget.take(1000, 10, 50));
        assertEquals(50, budget.take(1000, 11, 50));
    }

    @Test
    void zeroRequestGrantsNothing() {
        VoidBudget budget = new VoidBudget();
        assertEquals(0, budget.take(0, 0, 50));
        assertEquals(0, budget.take(-5, 0, 50));
    }
}
