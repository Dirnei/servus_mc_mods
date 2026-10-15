package io.github.dirnei.mc.servus.shuttles.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.power.PowerBudget.State;
import org.junit.jupiter.api.Test;

class PowerBudgetTest {

    @Test
    void drawLargerThanStoredIsRefusedAndNothingCounted() {
        PowerBudget budget = new PowerBudget();
        assertFalse(budget.draw(100, 15, 20));
        assertEquals(0, budget.usedLastSecond(100));
    }

    @Test
    void exactDrawIsPaid() {
        PowerBudget budget = new PowerBudget();
        assertTrue(budget.draw(100, 20, 20));
        assertEquals(20, budget.usedLastSecond(100));
        assertEquals(State.POWERED, budget.state(100, 1));
    }

    @Test
    void emptyBufferIsNoPower() {
        assertEquals(State.NO_POWER, new PowerBudget().state(5, 0));
        assertEquals(State.POWERED, new PowerBudget().state(5, 1));
    }

    @Test
    void refusalHoldsNoPowerForASecond() {
        PowerBudget budget = new PowerBudget();
        assertFalse(budget.draw(0, 10, 20));
        assertTrue(budget.draw(5, 500, 20));
        assertEquals(State.NO_POWER, budget.state(5, 480));
        assertEquals(State.NO_POWER, budget.state(19, 480));
        assertEquals(State.POWERED, budget.state(20, 480));
    }

    @Test
    void useSumsTheLastTwentyTicksAndForgetsOlder() {
        PowerBudget budget = new PowerBudget();
        budget.draw(0, 1000, 5);
        budget.draw(10, 1000, 7);
        budget.draw(10, 1000, 3);
        budget.draw(19, 1000, 1);
        assertEquals(16, budget.usedLastSecond(19));
        assertEquals(11, budget.usedLastSecond(20));
        assertEquals(1, budget.usedLastSecond(30));
        assertEquals(0, budget.usedLastSecond(39));
        budget.draw(40, 1000, 2);
        assertEquals(2, budget.usedLastSecond(40));
    }

    @Test
    void noNegativeValues() {
        PowerBudget budget = new PowerBudget();
        assertTrue(budget.draw(0, -5, 0));
        assertTrue(budget.draw(0, 10, -3));
        assertFalse(PowerBudget.paid(-5, 1));
        budget.use(0, -10);
        assertEquals(0, budget.usedLastSecond(0));
        assertEquals(State.NO_POWER, budget.state(0, -1));
    }
}
