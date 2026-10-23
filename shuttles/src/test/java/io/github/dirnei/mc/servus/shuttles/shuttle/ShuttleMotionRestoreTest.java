package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShuttleMotionRestoreTest {

    private static final List<NodePos> PATH = List.of(new NodePos(0, 0, 0), new NodePos(1, 0, 0), new NodePos(2, 0, 0));

    @Test
    void keepsSegmentAndProgress() {
        ShuttleMotion motion = ShuttleMotion.restored(PATH, 1, 0.25);
        assertEquals(1, motion.segment());
        assertEquals(0.25, motion.progress(), 1e-9);
        assertEquals(List.of(new NodePos(1, 0, 0), new NodePos(2, 0, 0)), motion.remaining());
    }

    @Test
    void clampsOutOfRange() {
        ShuttleMotion past = ShuttleMotion.restored(PATH, 7, 0.5);
        assertTrue(past.arrived());
        assertEquals(0, past.progress(), 1e-9);
        ShuttleMotion before = ShuttleMotion.restored(PATH, -3, 4.0);
        assertEquals(0, before.segment());
        assertTrue(before.progress() < 1);
    }

    @Test
    void oneNodePathIsArrived() {
        ShuttleMotion motion = ShuttleMotion.restored(List.of(new NodePos(5, 1, 5)), 0, 0.5);
        assertTrue(motion.arrived());
        assertEquals(List.of(new NodePos(5, 1, 5)), motion.remaining());
    }
}
