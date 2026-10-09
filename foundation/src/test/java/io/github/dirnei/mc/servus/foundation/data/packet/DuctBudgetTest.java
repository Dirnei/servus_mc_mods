package io.github.dirnei.mc.servus.foundation.data.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DuctBudgetTest {

    /** Ticks the budget for a second and lets each direction take up to {@code want} per tick; returns what passed. */
    private static int[] run(DuctBudget budget, int ticks, int wantForward, int wantBackward) {
        int[] passed = new int[2];
        for (int i = 0; i < ticks; i++) {
            budget.tick();
            int forward = Math.min(wantForward, budget.allowance(true));
            budget.consume(true, forward);
            int backward = Math.min(wantBackward, budget.allowance(false));
            budget.consume(false, backward);
            passed[0] += forward;
            passed[1] += backward;
        }
        return passed;
    }

    @Test
    void simplexSharesBothDirections() {
        DuctBudget budget = new DuctBudget(32, false);
        run(budget, 20, 64, 64);
        int[] second = run(budget, 20, 64, 64);
        assertEquals(32, second[0] + second[1]);
        assertEquals(32, budget.load());
        int[] mixed = run(budget, 20, 1, 64);
        assertEquals(20, mixed[0]);
        assertEquals(12, mixed[1], "the backward direction gets what the forward one leaves");
    }

    @Test
    void duplexSeparate() {
        DuctBudget budget = new DuctBudget(32, true);
        run(budget, 20, 64, 64);
        int[] second = run(budget, 20, 64, 64);
        assertEquals(32, second[0]);
        assertEquals(32, second[1]);
        assertEquals(32, budget.load());
    }

    @Test
    void fractionalCarry() {
        DuctBudget budget = new DuctBudget(10, true);
        // A new budget starts with one second's worth.
        assertEquals(10, budget.allowance(true));
        budget.consume(true, 10);
        budget.tick();
        assertEquals(0, budget.allowance(true));
        budget.tick();
        assertEquals(1, budget.allowance(true));
        int[] passed = run(budget, 40, 64, 0);
        assertEquals(21, passed[0], "the saved item plus half an item per tick");
    }

    @Test
    void loadLevels() {
        assertEquals(0, DuctBudget.level(0, 32));
        assertEquals(1, DuctBudget.level(4, 32));
        assertEquals(2, DuctBudget.level(8, 32));
        assertEquals(3, DuctBudget.level(24, 32));
        assertEquals(DuctBudget.FULL, DuctBudget.level(30, 32));
        assertEquals(DuctBudget.FULL, DuctBudget.level(40, 32));
    }

    @Test
    void loadWindow() {
        DuctBudget budget = new DuctBudget(32, true);
        assertTrue(budget.idle());
        budget.tick();
        budget.consume(true, 5);
        budget.consume(false, 3);
        assertEquals(5, budget.used(true));
        assertEquals(3, budget.used(false));
        assertEquals(5, budget.load());
        for (int i = 0; i < DuctBudget.WINDOW - 1; i++) {
            budget.tick();
        }
        assertEquals(5, budget.used(true), "still inside the last second");
        budget.tick();
        assertEquals(0, budget.used(true), "one second later it is forgotten");
        assertTrue(budget.idle());
        DuctBudget simplex = new DuctBudget(32, false);
        simplex.tick();
        simplex.consume(true, 5);
        simplex.consume(false, 3);
        assertEquals(8, simplex.load(), "simplex load counts both directions");
    }
}
