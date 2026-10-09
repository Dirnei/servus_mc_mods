package io.github.dirnei.mc.servus.foundation.data.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RateBudgetTest {

    /** Ticks the budget and spends everything it allows each tick; returns the items that passed. */
    private static int drain(RateBudget budget, int ticks) {
        int passed = 0;
        for (int i = 0; i < ticks; i++) {
            budget.tick();
            int now = budget.allowance();
            budget.consume(now);
            passed += now;
        }
        return passed;
    }

    @Test
    void eightPerSecondOverTwentyTicks() {
        RateBudget budget = new RateBudget(8);
        assertEquals(8, drain(budget, 20));
        assertEquals(8, drain(budget, 20));
        assertEquals(80, drain(new RateBudget(8), 200));
    }

    @Test
    void fractionalCarry() {
        RateBudget budget = new RateBudget(8);
        budget.tick();
        assertEquals(0, budget.allowance());
        budget.tick();
        assertEquals(0, budget.allowance());
        budget.tick();
        assertEquals(1, budget.allowance());
        budget.consume(1);
        assertEquals(0, budget.allowance());
        assertEquals(3, drain(new RateBudget(3), 20));
    }

    @Test
    void capAtOneSecond() {
        RateBudget budget = new RateBudget(8);
        for (int i = 0; i < 200; i++) {
            budget.tick();
        }
        assertEquals(8, budget.allowance());
        RateBudget slow = new RateBudget(0.5);
        for (int i = 0; i < 200; i++) {
            slow.tick();
        }
        assertEquals(1, slow.allowance());
        RateBudget stopped = new RateBudget(0);
        stopped.tick();
        assertEquals(0, stopped.allowance());
    }

    @Test
    void directionsIndependent() {
        RateBudget in = new RateBudget(8);
        RateBudget out = new RateBudget(8);
        int passedIn = 0;
        int passedOut = 0;
        for (int i = 0; i < 40; i++) {
            in.tick();
            out.tick();
            int now = in.allowance();
            in.consume(now);
            passedIn += now;
            if (i % 2 == 0) {
                int got = out.allowance();
                out.consume(got);
                passedOut += got;
            }
        }
        assertEquals(16, passedIn);
        assertEquals(16, passedOut + out.allowance());
    }
}
