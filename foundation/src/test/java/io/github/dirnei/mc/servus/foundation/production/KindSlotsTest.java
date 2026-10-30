package io.github.dirnei.mc.servus.foundation.production;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class KindSlotsTest {

    private static boolean accepts(int slot, String candidate, String... slots) {
        return KindSlots.accepts(List.of(slots), slot, candidate, String::isEmpty, String::equals);
    }

    @Test
    void fitsTheSlotHoldingTheSameKind() {
        assertTrue(accepts(1, "sand", "", "sand", "", ""));
    }

    @Test
    void fitsAnEmptySlotWhenNoOtherHoldsTheKind() {
        assertTrue(accepts(0, "sand", "", "gravel", "", ""));
        assertTrue(accepts(3, "sand", "", "", "", ""));
    }

    @Test
    void refusedIntoAnEmptySlotWhenAnotherHoldsTheKind() {
        assertFalse(accepts(0, "sand", "", "sand", "", ""));
    }

    @Test
    void refusedIntoASlotOfAnotherKind() {
        assertFalse(accepts(1, "sand", "", "gravel", "", ""));
    }
}
