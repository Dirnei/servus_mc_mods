package io.github.dirnei.mc.servus.shuttles.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReservationTallyTest {

    @Test
    void totalsSumOverSources() {
        ReservationTally<String, String> tally = new ReservationTally<>();
        tally.set("a", Map.of("cobble", 20));
        tally.set("b", Map.of("cobble", 5, "bread", 1));
        assertEquals(25, tally.reserved("cobble"));
        assertEquals(1, tally.reserved("bread"));
        assertEquals(0, tally.reserved("iron"));
    }

    @Test
    void replacingAReportLowersTheTotal() {
        ReservationTally<String, String> tally = new ReservationTally<>();
        tally.set("a", Map.of("cobble", 20));
        tally.set("a", Map.of("cobble", 8));
        assertEquals(8, tally.reserved("cobble"));
        tally.set("a", Map.of());
        assertEquals(0, tally.reserved("cobble"));
        assertTrue(tally.totals().isEmpty());
    }

    @Test
    void removeForgetsTheSource() {
        ReservationTally<String, String> tally = new ReservationTally<>();
        tally.set("a", Map.of("cobble", 20));
        tally.set("b", Map.of("cobble", 4));
        tally.remove("a");
        assertEquals(4, tally.reserved("cobble"));
        assertTrue(tally.of("a").isEmpty());
    }

    @Test
    void versionMovesOnlyOnChange() {
        ReservationTally<String, String> tally = new ReservationTally<>();
        long start = tally.version();
        tally.set("a", Map.of());
        tally.remove("a");
        assertEquals(start, tally.version());
        tally.set("a", Map.of("cobble", 3));
        long after = tally.version();
        assertNotEquals(start, after);
        tally.set("a", Map.of("cobble", 3));
        assertEquals(after, tally.version());
        tally.remove("a");
        assertNotEquals(after, tally.version());
    }

    @Test
    void absorbTakesOtherSources() {
        ReservationTally<String, String> tally = new ReservationTally<>();
        ReservationTally<String, String> other = new ReservationTally<>();
        tally.set("a", Map.of("cobble", 2));
        other.set("b", Map.of("cobble", 3));
        tally.absorb(other);
        assertEquals(5, tally.reserved("cobble"));
        assertEquals(Map.of("cobble", 3), tally.of("b"));
    }

    @Test
    void requestableSubtractsDropsAndSorts() {
        List<Map.Entry<String, Long>> stock = List.of(Map.entry("cobble", 50L), Map.entry("bread", 30L), Map.entry("key", 1L));
        List<Map.Entry<String, Long>> left = ReservationTally.requestable(stock, Map.of("cobble", 40L, "key", 1L));
        assertEquals(List.of(Map.entry("bread", 30L), Map.entry("cobble", 10L)), left);
    }

    @Test
    void requestableIsTheSameListWithoutReservations() {
        List<Map.Entry<String, Long>> stock = List.of(Map.entry("cobble", 50L));
        assertSame(stock, ReservationTally.requestable(stock, Map.of()));
    }
}
