package io.github.dirnei.mc.servus.alchemy.sight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ElixirDoseTest {

    private static final double ALMOST_ONE = Math.nextDown(1.0);

    @Test
    void chanceZeroNeverPoisons() {
        ElixirDose dose = new ElixirDose(0, 0, 200, 200);
        assertFalse(dose.poisons(false, 0));
        assertFalse(dose.poisons(false, ALMOST_ONE));
    }

    @Test
    void chanceOneAlwaysPoisons() {
        ElixirDose dose = new ElixirDose(1, 0, 200, 200);
        assertTrue(dose.poisons(false, 0));
        assertTrue(dose.poisons(false, ALMOST_ONE));
    }

    @Test
    void rollBelowChancePoisons() {
        ElixirDose dose = new ElixirDose(0.15, 0, 200, 200);
        assertTrue(dose.poisons(false, 0.14));
        assertFalse(dose.poisons(false, 0.15));
        assertFalse(dose.poisons(false, 0.5));
    }

    @Test
    void sightedAddsExtraChance() {
        ElixirDose dose = new ElixirDose(0.15, 0.15, 200, 200);
        assertEquals(0.15, dose.chance(false), 1e-9);
        assertEquals(0.30, dose.chance(true), 1e-9);
        assertFalse(dose.poisons(false, 0.2));
        assertTrue(dose.poisons(true, 0.2));
    }

    @Test
    void extraChanceIsCapped() {
        ElixirDose dose = new ElixirDose(0.8, 0.5, 200, 200);
        assertEquals(1.0, dose.chance(true), 1e-9);
        assertTrue(dose.poisons(true, ALMOST_ONE));
    }

    @Test
    void harmlessNeverPoisons() {
        assertFalse(ElixirDose.HARMLESS.poisons(true, 0));
    }

    @Test
    void rejectsChanceOutsideRange() {
        assertThrows(IllegalArgumentException.class, () -> new ElixirDose(1.5, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ElixirDose(0.1, -0.1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ElixirDose(Double.NaN, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ElixirDose(0.1, 0, -1, 0));
    }
}
