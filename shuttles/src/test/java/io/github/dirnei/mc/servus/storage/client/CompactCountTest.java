package io.github.dirnei.mc.servus.storage.client;

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
    void millionsBecomeM() {
        assertEquals("1.0M", CompactCount.of(1_000_000));
        assertEquals("1.2M", CompactCount.of(1_299_999));
        assertEquals("9.9M", CompactCount.of(9_999_999));
        assertEquals("12M", CompactCount.of(12_345_678));
    }
}
