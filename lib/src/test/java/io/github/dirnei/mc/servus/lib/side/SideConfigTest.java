package io.github.dirnei.mc.servus.lib.side;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SideConfigTest {

    @Test
    void cycleOrder() {
        SideConfig config = new SideConfig();
        assertEquals(SideMode.INPUT, config.cycle(RelativeSide.TOP));
        assertEquals(SideMode.OUTPUT, config.cycle(RelativeSide.TOP));
        assertEquals(SideMode.BOTH, config.cycle(RelativeSide.TOP));
        assertEquals(SideMode.DISABLED, config.cycle(RelativeSide.TOP));
        assertEquals(SideMode.AUTO, config.cycle(RelativeSide.TOP));
    }

    @Test
    void permissions() {
        assertTrue(SideMode.AUTO.allowsInsert() && SideMode.AUTO.allowsExtract());
        assertTrue(SideMode.INPUT.allowsInsert() && !SideMode.INPUT.allowsExtract());
        assertTrue(!SideMode.OUTPUT.allowsInsert() && SideMode.OUTPUT.allowsExtract());
        assertTrue(SideMode.BOTH.allowsInsert() && SideMode.BOTH.allowsExtract());
        assertFalse(SideMode.DISABLED.allowsInsert() || SideMode.DISABLED.allowsExtract());
    }

    @Test
    void defaultIsAuto() {
        SideConfig config = new SideConfig();
        assertTrue(config.isDefault());
        for (RelativeSide side : RelativeSide.values()) {
            assertEquals(SideMode.AUTO, config.mode(side));
        }
        config.set(RelativeSide.LEFT, SideMode.DISABLED);
        assertFalse(config.isDefault());
    }

    @Test
    void byteRoundTrip() {
        SideConfig config = new SideConfig();
        config.set(RelativeSide.FRONT, SideMode.OUTPUT);
        config.set(RelativeSide.BOTTOM, SideMode.DISABLED);
        byte[] bytes = config.toBytes();
        assertArrayEquals(new byte[] {2, 0, 0, 0, 0, 4}, bytes);

        SideConfig copy = new SideConfig();
        copy.fromBytes(bytes);
        assertEquals(SideMode.OUTPUT, copy.mode(RelativeSide.FRONT));
        assertEquals(SideMode.DISABLED, copy.mode(RelativeSide.BOTTOM));
        assertEquals(SideMode.AUTO, copy.mode(RelativeSide.LEFT));

        copy.fromBytes(new byte[] {9});
        assertEquals(SideMode.AUTO, copy.mode(RelativeSide.FRONT));
    }
}
