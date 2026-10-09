package io.github.dirnei.mc.servus.foundation.machine.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SinkRatesTest {

    private static final SinkRates RATES = new SinkRates(
            List.of(4, 8, 16, 32, 64),
            List.of(50, 100, 200, 400, 800),
            List.of(100, 200, 400, 800, 1600));

    @Test
    void heightZeroUsesTheBaseRates() {
        assertEquals(4 / 20.0, RATES.items(0), 1e-9);
        assertEquals(50, RATES.fluid(0), 1e-9);
        assertEquals(100, RATES.energy(0), 1e-9);
        assertEquals(4, RATES.itemsDisplay(0));
    }

    @Test
    void lastLimitedHeightUsesTheLastEntry() {
        assertFalse(RATES.isUnlimited(4));
        assertEquals(64 / 20.0, RATES.items(4), 1e-9);
        assertEquals(800, RATES.fluid(4), 1e-9);
        assertEquals(1600, RATES.energy(4), 1e-9);
    }

    @Test
    void unlimitedHeightIsTheTableLength() {
        assertEquals(5, RATES.unlimitedHeight());
        assertTrue(RATES.isUnlimited(5));
        assertEquals(SinkRates.UNLIMITED, RATES.items(5));
        assertEquals(SinkRates.UNLIMITED, RATES.fluid(5));
        assertEquals(SinkRates.UNLIMITED, RATES.energy(5));
        assertEquals(SinkRates.UNLIMITED_DISPLAY, RATES.itemsDisplay(5));
        assertEquals(SinkRates.UNLIMITED_DISPLAY, RATES.fluidDisplay(5));
        assertEquals(SinkRates.UNLIMITED_DISPLAY, RATES.energyDisplay(5));
    }

    @Test
    void heightsAboveStayUnlimited() {
        assertEquals(SinkRates.UNLIMITED, RATES.items(9));
        assertEquals(SinkRates.UNLIMITED, RATES.energy(32));
    }

    @Test
    void shortestTableSetsTheUnlimitedHeight() {
        SinkRates uneven = new SinkRates(List.of(1, 2, 3), List.of(10, 20), List.of(5, 6, 7, 8));
        assertEquals(2, uneven.unlimitedHeight());
        assertEquals(2 / 20.0, uneven.items(1), 1e-9);
        assertEquals(SinkRates.UNLIMITED, uneven.items(2));
        assertEquals(SinkRates.UNLIMITED, uneven.energy(2));
    }

    @Test
    void emptyTablesAreUnlimitedFromTheStart() {
        SinkRates none = new SinkRates(List.of(), List.of(1), List.of(1));
        assertEquals(0, none.unlimitedHeight());
        assertEquals(SinkRates.UNLIMITED, none.fluid(0));
    }
}
