package io.github.dirnei.mc.servus.shuttles.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.station.StationCounters.Kind;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StationCountersTest {

    private static final UUID HERE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID THERE = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void countsPerKindFromOne() {
        StationCounters counters = new StationCounters();
        assertEquals(0, counters.last(Kind.OUTBOUND));
        assertEquals(1, counters.take(Kind.OUTBOUND));
        assertEquals(2, counters.take(Kind.OUTBOUND));
        assertEquals(1, counters.take(Kind.INBOUND));
    }

    @Test
    void neverReusesANumber() {
        StationCounters counters = new StationCounters();
        counters.take(Kind.OUTBOUND);
        counters.take(Kind.OUTBOUND);
        assertEquals(3, counters.take(Kind.OUTBOUND));
        assertEquals(3, counters.last(Kind.OUTBOUND));
    }

    @Test
    void kindsAreIndependent() {
        StationCounters counters = new StationCounters();
        counters.take(Kind.INBOUND);
        counters.take(Kind.INBOUND);
        assertEquals(2, counters.last(Kind.INBOUND));
        assertEquals(0, counters.last(Kind.OUTBOUND));
        assertEquals(0, counters.last(Kind.PACKING));
    }

    @Test
    void packingCountsOnItsOwn() {
        StationCounters counters = new StationCounters();
        counters.take(Kind.INBOUND);
        assertEquals(1, counters.take(Kind.PACKING));
        assertEquals(2, counters.take(Kind.INBOUND));
    }

    @Test
    void saveRoundTripContinuesTheCount() {
        StationCounters counters = new StationCounters();
        counters.take(Kind.INBOUND);
        counters.take(Kind.OUTBOUND);
        counters.take(Kind.OUTBOUND);
        counters.take(Kind.PACKING);
        StationCounters loaded = new StationCounters(counters.last(Kind.INBOUND), counters.last(Kind.OUTBOUND), counters.last(Kind.PACKING));
        assertEquals(2, loaded.take(Kind.INBOUND));
        assertEquals(3, loaded.take(Kind.OUTBOUND));
        assertEquals(2, loaded.take(Kind.PACKING));
    }

    @Test
    void copyContinuesTheOtherCount() {
        StationCounters carried = new StationCounters(4, 7, 2);
        StationCounters placed = new StationCounters();
        placed.copyFrom(carried);
        assertEquals(5, placed.take(Kind.INBOUND));
        assertEquals(8, placed.take(Kind.OUTBOUND));
        assertEquals(3, placed.take(Kind.PACKING));
    }

    @Test
    void negativeSavedCountsStartAtOne() {
        assertEquals(1, new StationCounters(-3, -1, -2).take(Kind.INBOUND));
        assertEquals(1, new StationCounters(-3, -1, -2).take(Kind.PACKING));
    }

    @Test
    void savedCounterNameIsKept() {
        assertEquals(Kind.PACKING, StationCounters.counterOf("PACKING", Kind.PACKING));
        assertEquals(Kind.INBOUND, StationCounters.counterOf("INBOUND", Kind.PACKING));
        assertEquals(Kind.OUTBOUND, StationCounters.counterOf("OUTBOUND", Kind.OUTBOUND));
    }

    @Test
    void numberSavedWithoutCounterCameFromInboundForPacking() {
        assertEquals(Kind.INBOUND, StationCounters.counterOf("", Kind.PACKING));
        assertEquals(Kind.INBOUND, StationCounters.counterOf("", Kind.INBOUND));
        assertEquals(Kind.OUTBOUND, StationCounters.counterOf("", Kind.OUTBOUND));
        assertEquals(Kind.OUTBOUND, StationCounters.counterOf("nonsense", Kind.OUTBOUND));
    }

    @Test
    void needsANumberWhenUnnumbered() {
        assertTrue(StationCounters.needsNumber(null, 0, HERE));
        assertTrue(StationCounters.needsNumber(HERE, 0, HERE));
    }

    @Test
    void needsANumberFromAnotherWarehouse() {
        assertTrue(StationCounters.needsNumber(THERE, 3, HERE));
        assertTrue(StationCounters.needsNumber(null, 3, HERE));
    }

    @Test
    void keepsTheNumberOfThisWarehouse() {
        assertFalse(StationCounters.needsNumber(HERE, 3, HERE));
    }
}
