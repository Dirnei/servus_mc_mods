package io.github.dirnei.mc.servus.shuttles.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CompactCountTest {

    @Test
    void smallCountsAreGrouped() {
        assertEquals("0", CompactCount.of(0));
        assertEquals("1,024", CompactCount.of(1_024));
        assertEquals("99,999", CompactCount.of(99_999));
    }

    @Test
    void thousandsBecomeK() {
        assertEquals("100k", CompactCount.of(100_000));
        assertEquals("999k", CompactCount.of(999_999));
    }

    @Test
    void slotCountsStayShort() {
        assertEquals("999", CompactCount.slot(999));
        assertEquals("8k", CompactCount.slot(8_000));
        assertEquals("8.5k", CompactCount.slot(8_560));
        assertEquals("12k", CompactCount.slot(12_900));
        assertEquals("999k", CompactCount.slot(999_999));
        assertEquals("1.2M", CompactCount.slot(1_250_000));
        assertEquals("15M", CompactCount.slot(15_000_000));
    }

    @Test
    void millionsBecomeM() {
        assertEquals("1.0M", CompactCount.of(1_000_000));
        assertEquals("1.2M", CompactCount.of(1_299_999));
        assertEquals("9.9M", CompactCount.of(9_999_999));
        assertEquals("12M", CompactCount.of(12_345_678));
    }
}
