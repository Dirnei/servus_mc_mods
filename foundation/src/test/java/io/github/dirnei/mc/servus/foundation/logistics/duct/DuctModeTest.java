package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DuctModeTest {

    @Test
    void flagsRoundTrip() {
        for (DuctMode mode : DuctMode.values()) {
            assertEquals(mode, DuctMode.of(mode.in(), mode.out(), mode.pull()));
        }
    }

    @Test
    void pullNeedsIn() {
        assertEquals(DuctMode.OUT, DuctMode.of(false, true, true));
        assertEquals(DuctMode.NONE, DuctMode.of(false, false, true));
        for (DuctMode mode : DuctMode.values()) {
            assertTrue(!mode.pull() || mode.in(), mode + " pulls only with In");
        }
    }

    @Test
    void toggles() {
        assertEquals(DuctMode.IN_OUT_PULL, DuctMode.IN_OUT.togglePull());
        assertEquals(DuctMode.IN, DuctMode.IN_OUT.toggleOut());
        assertEquals(DuctMode.OUT, DuctMode.IN_OUT_PULL.toggleIn());
        assertEquals(DuctMode.IN_OUT, DuctMode.OUT.toggleIn());
        assertEquals(DuctMode.NONE, DuctMode.OUT.toggleOut());
        assertEquals(DuctMode.IN_OUT_PULL, DuctMode.OUT.togglePull());
        assertEquals(DuctMode.IN_PULL, DuctMode.NONE.togglePull());
        assertEquals(DuctMode.IN, DuctMode.IN_PULL.togglePull());
    }

    @Test
    void onlyNoneDisconnects() {
        for (DuctMode mode : DuctMode.values()) {
            assertEquals(mode != DuctMode.NONE, mode.connects());
        }
    }

    @Test
    void lineNormalisation() {
        assertEquals(DuctMode.IN_OUT_PULL, DuctMode.IN_OUT_PULL.forLine(Line.ITEM));
        assertEquals(DuctMode.IN_OUT, DuctMode.IN_OUT_PULL.forLine(Line.FLUID));
        assertEquals(DuctMode.IN, DuctMode.IN_PULL.forLine(Line.ENERGY));
        assertEquals(DuctMode.IN_OUT, DuctMode.OUT.forLine(Line.DATA));
        assertEquals(DuctMode.NONE, DuctMode.NONE.forLine(Line.DATA));
    }

    @Test
    void savedNamesRoundTrip() {
        for (DuctMode mode : DuctMode.values()) {
            assertEquals(mode, DuctMode.bySaved(mode.key()));
        }
    }

    @Test
    void olderModeNamesLoadAsFlags() {
        assertEquals(DuctMode.IN_OUT, DuctMode.bySaved("both"));
        assertEquals(DuctMode.IN_OUT, DuctMode.bySaved("auto"));
        assertEquals(DuctMode.IN_PULL, DuctMode.bySaved("input"));
        assertEquals(DuctMode.IN_PULL, DuctMode.bySaved("pull"));
        assertEquals(DuctMode.OUT, DuctMode.bySaved("output"));
        assertEquals(DuctMode.OUT, DuctMode.bySaved("push"));
        assertEquals(DuctMode.NONE, DuctMode.bySaved("disabled"));
        assertEquals(DuctMode.IN_OUT, DuctMode.bySaved("something"));
        assertFalse(DuctMode.bySaved("input").out());
    }
}
