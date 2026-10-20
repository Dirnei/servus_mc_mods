package io.github.dirnei.mc.servus.foundation.machine.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SinkCapacityTest {

    private static final SinkCapacity CAPACITY = new SinkCapacity(7, 3);

    @Test
    void eachUpgradeOpensARowAndATank() {
        for (int upgrades = 0; upgrades <= 3; upgrades++) {
            assertEquals(7 * (upgrades + 1), CAPACITY.openSlots(upgrades));
            assertEquals(upgrades + 1, CAPACITY.openTanks(upgrades));
        }
        assertEquals(28, CAPACITY.maxSlots());
        assertEquals(4, CAPACITY.maxRows());
    }

    @Test
    void upgradesBeyondTheSlotsOpenNothingMore() {
        assertEquals(28, CAPACITY.openSlots(9));
    }

    @Test
    void slotsBelongToTheirRow() {
        assertEquals(0, CAPACITY.rowOf(6));
        assertEquals(1, CAPACITY.rowOf(7));
        assertEquals(3, CAPACITY.rowOf(27));
    }

    @Test
    void removalRefusedWithItemsInTheLastRow() {
        assertFalse(SinkCapacity.canRemoveUpgrade(1, false, true));
    }

    @Test
    void removalRefusedWithFluidInTheLastTank() {
        assertFalse(SinkCapacity.canRemoveUpgrade(2, true, false));
    }

    @Test
    void removalAllowedWhenBothAreEmpty() {
        assertTrue(SinkCapacity.canRemoveUpgrade(1, true, true));
    }

    @Test
    void removalRefusedWithoutUpgrades() {
        assertFalse(SinkCapacity.canRemoveUpgrade(0, true, true));
    }
}
