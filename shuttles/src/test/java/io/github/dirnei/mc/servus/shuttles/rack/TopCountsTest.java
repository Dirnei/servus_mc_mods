package io.github.dirnei.mc.servus.shuttles.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TopCountsTest {

    private static Map<String, Integer> counts(Object... pairs) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }

    @Test
    void fewerTypesThanWantedReturnsAllSorted() {
        assertEquals(List.of("cobble", "dirt"), TopCounts.pick(counts("dirt", 32, "cobble", 64), 4));
    }

    @Test
    void moreTypesThanWantedKeepsTheLargest() {
        Map<String, Integer> map = counts("cobble", 64, "dirt", 32, "sand", 16, "gravel", 8, "apple", 1);
        assertEquals(List.of("cobble", "dirt", "sand", "gravel"), TopCounts.pick(map, 4));
    }

    @Test
    void tiesKeepInsertionOrder() {
        assertEquals(List.of("first", "second"), TopCounts.pick(counts("first", 5, "second", 5, "third", 5), 2));
    }

    @Test
    void emptyAndZeroCountsGiveNothing() {
        assertTrue(TopCounts.pick(counts(), 4).isEmpty());
        assertTrue(TopCounts.pick(counts("air", 0), 4).isEmpty());
        assertTrue(TopCounts.pick(counts("cobble", 3), 0).isEmpty());
    }
}
