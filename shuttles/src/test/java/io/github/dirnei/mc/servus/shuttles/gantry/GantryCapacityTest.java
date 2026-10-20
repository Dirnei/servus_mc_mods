package io.github.dirnei.mc.servus.shuttles.gantry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GantryCapacityTest {

    @Test
    void capacityGrowsPerUpgrade() {
        assertEquals(16000, GantryCapacity.capacity(16000, 16000, 0));
        assertEquals(48000, GantryCapacity.capacity(16000, 16000, 2));
        assertEquals(Integer.MAX_VALUE, GantryCapacity.capacity(Integer.MAX_VALUE, 16000, 4));
    }

    @Test
    void removalRefusedWhenContentWouldNotFit() {
        assertFalse(GantryCapacity.canRemoveUpgrade(16000, 16000, 1, 20000));
        assertFalse(GantryCapacity.canRemoveUpgrade(16000, 16000, 0, 0));
    }

    @Test
    void removalAllowedAtExactFit() {
        assertTrue(GantryCapacity.canRemoveUpgrade(16000, 16000, 1, 16000));
        assertTrue(GantryCapacity.canRemoveUpgrade(16000, 16000, 2, 32000));
    }
}
