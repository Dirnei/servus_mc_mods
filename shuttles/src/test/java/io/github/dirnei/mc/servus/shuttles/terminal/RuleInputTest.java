package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RuleInputTest {

    @Test
    void eightToSixteenIsValid() {
        assertNull(RuleInput.refusal(8, 16));
    }

    @Test
    void minimumEqualToMaximumIsRefused() {
        assertEquals(RuleInput.ORDER, RuleInput.refusal(16, 16));
        assertEquals(RuleInput.ORDER, RuleInput.refusal(20, 16));
    }

    @Test
    void zeroToOneIsValid() {
        assertNull(RuleInput.refusal(0, 1));
    }

    @Test
    void maximumZeroIsRefused() {
        assertEquals(RuleInput.MAX_LOW, RuleInput.refusal(0, 0));
    }

    @Test
    void negativeMinimumIsRefused() {
        assertEquals(RuleInput.MIN_LOW, RuleInput.refusal(-1, 16));
    }
}
