package io.github.dirnei.mc.servus.lib.side;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SideConfigTest {

    @Test
    void machineCycleOrder() {
        SideConfig config = SideConfig.machine();
        assertEquals(SideMode.INPUT, config.cycle(RelativeSide.TOP));
        assertEquals(SideMode.OUTPUT, config.cycle(RelativeSide.TOP));
        assertEquals(SideMode.BOTH, config.cycle(RelativeSide.TOP));
        assertEquals(SideMode.DISABLED, config.cycle(RelativeSide.TOP));
        assertEquals(SideMode.PULL, config.cycle(RelativeSide.TOP));
        assertEquals(SideMode.PUSH, config.cycle(RelativeSide.TOP));
        assertEquals(SideMode.AUTO, config.cycle(RelativeSide.TOP));
    }

    @Test
    void stationCycleStaysInItsSet() {
        SideConfig inbound = SideConfig.inbound();
        assertEquals(SideMode.INPUT, inbound.mode(RelativeSide.LEFT));
        assertEquals(SideMode.PULL, inbound.cycle(RelativeSide.LEFT));
        assertEquals(SideMode.DISABLED, inbound.cycle(RelativeSide.LEFT));
        assertEquals(SideMode.INPUT, inbound.cycle(RelativeSide.LEFT));

        SideConfig outbound = SideConfig.outbound();
        assertEquals(SideMode.DISABLED, outbound.mode(RelativeSide.FRONT));
        assertEquals(SideMode.PUSH, outbound.cycle(RelativeSide.FRONT));
        assertEquals(SideMode.OUTPUT, outbound.cycle(RelativeSide.FRONT));
        assertEquals(SideMode.DISABLED, outbound.cycle(RelativeSide.FRONT));
    }

    @Test
    void unsupportedModeFallsBack() {
        SideConfig outbound = SideConfig.outbound();
        outbound.set(RelativeSide.TOP, SideMode.BOTH);
        assertEquals(SideMode.DISABLED, outbound.mode(RelativeSide.TOP));
        assertThrows(IllegalArgumentException.class, () -> new SideConfig(List.of(SideMode.INPUT), SideMode.PUSH));
    }

    @Test
    void permissions() {
        assertTrue(SideMode.AUTO.allowsInsert() && SideMode.AUTO.allowsExtract() && SideMode.AUTO.pushes() && !SideMode.AUTO.pulls());
        assertTrue(SideMode.INPUT.allowsInsert() && !SideMode.INPUT.allowsExtract() && !SideMode.INPUT.pulls());
        assertTrue(!SideMode.OUTPUT.allowsInsert() && SideMode.OUTPUT.allowsExtract() && !SideMode.OUTPUT.pushes());
        assertTrue(SideMode.BOTH.allowsInsert() && SideMode.BOTH.allowsExtract() && !SideMode.BOTH.pushes());
        assertTrue(SideMode.PULL.allowsInsert() && !SideMode.PULL.allowsExtract() && SideMode.PULL.pulls());
        assertTrue(!SideMode.PUSH.allowsInsert() && SideMode.PUSH.allowsExtract() && SideMode.PUSH.pushes());
        assertFalse(SideMode.DISABLED.allowsInsert() || SideMode.DISABLED.allowsExtract() || SideMode.DISABLED.pulls() || SideMode.DISABLED.pushes());
        assertEquals("off", SideMode.DISABLED.key());
    }

    @Test
    void lockedSideStays() {
        SideConfig config = SideConfig.machine();
        config.lock(RelativeSide.FRONT, SideMode.DISABLED);
        assertTrue(config.isLocked(RelativeSide.FRONT) && !config.isLocked(RelativeSide.BACK));
        assertEquals(SideMode.DISABLED, config.cycle(RelativeSide.FRONT));
        config.set(RelativeSide.FRONT, SideMode.INPUT);
        assertEquals(SideMode.DISABLED, config.mode(RelativeSide.FRONT));
        config.fromBytes(new byte[] {1, 1, 0, 0, 0, 0});
        assertEquals(SideMode.DISABLED, config.mode(RelativeSide.FRONT));
        assertEquals(SideMode.INPUT, config.mode(RelativeSide.BACK));
        config.set(RelativeSide.BACK, SideMode.AUTO);
        assertTrue(config.isDefault(), "a locked side does not count against the default");
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
        config.set(RelativeSide.LEFT, SideMode.PUSH);
        byte[] bytes = config.toBytes();
        assertArrayEquals(new byte[] {2, 0, 6, 0, 0, 4}, bytes);

        SideConfig copy = new SideConfig();
        copy.fromBytes(bytes);
        assertEquals(SideMode.OUTPUT, copy.mode(RelativeSide.FRONT));
        assertEquals(SideMode.DISABLED, copy.mode(RelativeSide.BOTTOM));
        assertEquals(SideMode.PUSH, copy.mode(RelativeSide.LEFT));
        assertEquals(SideMode.AUTO, copy.mode(RelativeSide.RIGHT));

        copy.fromBytes(new byte[] {9});
        assertEquals(SideMode.AUTO, copy.mode(RelativeSide.FRONT));
    }
}
