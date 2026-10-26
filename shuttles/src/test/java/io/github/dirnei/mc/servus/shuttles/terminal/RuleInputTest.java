package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuleInputTest {

    @Test
    void eightToSixteenIsValid() {
        RuleInput input = RuleInput.of(8, 16, 0, -1);
        assertNull(input.refusal());
        assertTrue(input.hasMake());
        assertEquals(new RuleInput(8, 16, 0, -1), input);
    }

    @Test
    void minimumEqualToMaximumIsRefused() {
        assertEquals("rule_make_order", RuleInput.of(16, 16, 0, -1).refusal());
    }

    @Test
    void zeroToOneIsValid() {
        assertNull(RuleInput.of(0, 1, 0, -1).refusal());
    }

    @Test
    void fillUpToOffWithAMinimumIsRefused() {
        assertEquals("rule_make_order", RuleInput.of(8, 0, 4096, -1).refusal());
    }

    @Test
    void makeOffWithACap() {
        RuleInput input = RuleInput.of(0, 0, 4096, -1);
        assertFalse(input.hasMake());
        assertEquals(new RuleInput(-1, -1, 4096, -1), input);
        assertNull(input.refusal());
    }

    @Test
    void nothingSetIsRefused() {
        assertEquals("rule_empty", RuleInput.of(0, 0, 0, -1).refusal());
    }

    @Test
    void exportOnlyFromZero() {
        assertNull(RuleInput.of(0, 0, 0, 0).refusal());
        assertEquals(new RuleInput(-1, -1, 0, 2000), RuleInput.of(0, 0, 0, 2000));
    }

    @Test
    void exportNotOverFillUpToIsRefused() {
        assertEquals("rule_export_not_above", RuleInput.of(100, 500, 0, 400).refusal());
    }

    @Test
    void capBelowFillUpToIsRefused() {
        assertEquals("rule_cap_below_max", RuleInput.of(8, 16, 10, -1).refusal());
    }
}
