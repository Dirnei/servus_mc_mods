package io.github.dirnei.mc.servus.storage.picker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PickerStockTest {

    private static Map<String, Long> counts(Object... pairs) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], ((Number) pairs[i + 1]).longValue());
        }
        return map;
    }

    @Test
    void signatureIgnoresOrder() {
        assertEquals(PickerStock.signature(true, counts("a", 1, "b", 2)), PickerStock.signature(true, counts("b", 2, "a", 1)));
    }

    @Test
    void signatureFollowsCountsAndNetwork() {
        long base = PickerStock.signature(true, counts("a", 1, "b", 2));
        assertNotEquals(base, PickerStock.signature(true, counts("a", 1, "b", 3)));
        assertNotEquals(base, PickerStock.signature(true, counts("a", 1)));
        assertNotEquals(PickerStock.signature(true, counts()), PickerStock.signature(false, counts()));
    }

    @Test
    void shortCounts() {
        assertEquals("0", PickerStock.shortCount(0));
        assertEquals("999", PickerStock.shortCount(999));
        assertEquals("1k", PickerStock.shortCount(1000));
        assertEquals("1.2k", PickerStock.shortCount(1234));
        assertEquals("12k", PickerStock.shortCount(12_345));
        assertEquals("999k", PickerStock.shortCount(999_999));
        assertEquals("1.5M", PickerStock.shortCount(1_500_000));
        assertEquals("2G", PickerStock.shortCount(2_000_000_000L));
    }
}
