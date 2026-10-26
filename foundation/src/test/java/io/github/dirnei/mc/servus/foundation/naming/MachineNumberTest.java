package io.github.dirnei.mc.servus.foundation.naming;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MachineNumberTest {

    private static final UUID HERE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID THERE = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void unnumberedNeedsANumber() {
        assertTrue(MachineNumber.needsNumber(HERE, 0, HERE));
    }

    @Test
    void numberFromAnotherWarehouseNeedsANewOne() {
        assertTrue(MachineNumber.needsNumber(THERE, 4, HERE));
    }

    @Test
    void numberFromThisWarehouseIsKept() {
        assertFalse(MachineNumber.needsNumber(HERE, 4, HERE));
    }

    @Test
    void numberWithoutWarehouseNeedsANewOne() {
        assertTrue(MachineNumber.needsNumber(null, 4, HERE));
    }

    @Test
    void noCurrentWarehouseMeansNoMatch() {
        assertTrue(MachineNumber.needsNumber(HERE, 4, null));
    }
}
