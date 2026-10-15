package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldDueTest {

    @Test
    void harvestDueIsEnough() {
        assertTrue(FieldDue.due(true, false, false, 0, 0, false));
    }

    @Test
    void tillingNeedsPlantModeAndWater() {
        assertTrue(FieldDue.due(false, true, true, 1, 0, false));
        assertFalse(FieldDue.due(false, false, true, 1, 0, true));
        assertFalse(FieldDue.due(false, true, false, 1, 0, true));
    }

    @Test
    void plantingNeedsSeedsInTheWarehouse() {
        assertTrue(FieldDue.due(false, true, true, 0, 3, true));
        assertFalse(FieldDue.due(false, true, true, 0, 3, false));
    }

    @Test
    void nothingToDo() {
        assertFalse(FieldDue.due(false, true, true, 0, 0, true));
    }
}
