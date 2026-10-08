package io.github.dirnei.mc.servus.storage.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AccessPointChoiceTest {

    private static final int NONE = AccessPointChoice.UNREACHABLE;

    private static String choose(Map<String, Integer> trips, String... order) {
        return AccessPointChoice.choose(List.of(order), trips::get).orElseThrow();
    }

    @Test
    void lowestTripWins() {
        assertEquals("b", choose(Map.of("a", 8, "b", 2, "c", 5), "a", "b", "c"));
    }

    @Test
    void tieKeepsFirst() {
        assertEquals("a", choose(Map.of("a", 0, "b", 0), "a", "b"));
    }

    @Test
    void unreachableLoses() {
        assertEquals("b", choose(Map.of("a", NONE, "b", 30), "a", "b"));
    }

    @Test
    void allUnreachablePicksFirst() {
        assertEquals("a", choose(Map.of("a", NONE, "b", NONE), "a", "b"));
    }

    @Test
    void emptyHasNone() {
        assertTrue(AccessPointChoice.<String>choose(List.of(), s -> 0).isEmpty());
    }
}
