package io.github.dirnei.mc.servus.storage.shuttle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShuttleMotionTest {

    private static NodePos n(int x, int z) {
        return new NodePos(x, 0, z);
    }

    @Test
    void advancesAcrossSegments() {
        ShuttleMotion motion = new ShuttleMotion(List.of(n(0, 0), n(1, 0), n(2, 0)));
        assertFalse(motion.advance(0.5));
        assertEquals(0, motion.segment());
        assertEquals(0.5, motion.progress(), 1e-9);
        assertFalse(motion.advance(1.0));
        assertEquals(1, motion.segment());
        assertEquals(0.5, motion.progress(), 1e-9);
        assertTrue(motion.advance(0.5));
        assertTrue(motion.arrived());
        assertEquals(n(2, 0), motion.currentNode());
    }

    @Test
    void oneNodePathHasArrived() {
        ShuttleMotion parked = ShuttleMotion.parked(n(4, 4));
        assertTrue(parked.arrived());
        assertTrue(parked.advance(3.0));
        assertArrayEquals(new double[] {4, 0, 4}, parked.position(), 1e-9);
    }

    @Test
    void positionInterpolates() {
        ShuttleMotion motion = new ShuttleMotion(List.of(n(0, 0), n(0, 4)));
        motion.advance(0.25);
        assertArrayEquals(new double[] {0, 0, 1.0}, motion.position(), 1e-9);
    }

    @Test
    void reverseGoesHome() {
        ShuttleMotion motion = new ShuttleMotion(List.of(n(0, 0), n(1, 0), n(2, 0)));
        motion.advance(5.0);
        ShuttleMotion back = motion.reverse();
        assertEquals(n(2, 0), back.start());
        assertEquals(n(0, 0), back.end());
        assertFalse(back.arrived());
    }
}
