package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FieldRadiusTest {

    @Test
    void baseWithoutUpgrades() {
        assertEquals(2, FieldRadius.of(2, 4, 0));
    }

    @Test
    void oneBlockPerUpgrade() {
        assertEquals(3, FieldRadius.of(2, 4, 1));
        assertEquals(4, FieldRadius.of(2, 4, 2));
    }

    @Test
    void neverPastTheMaximum() {
        assertEquals(4, FieldRadius.of(2, 4, 5));
    }

    @Test
    void maximumBelowBaseCountsAsBase() {
        assertEquals(3, FieldRadius.of(3, 1, 2));
        assertEquals(0, FieldRadius.maxUpgrades(3, 1));
    }

    @Test
    void upgradesUpToTheMaximum() {
        assertEquals(2, FieldRadius.maxUpgrades(2, 4));
        assertEquals(0, FieldRadius.maxUpgrades(4, 4));
    }

    @Test
    void negativeInputsAreZero() {
        assertEquals(0, FieldRadius.of(-1, -1, -3));
    }
}
