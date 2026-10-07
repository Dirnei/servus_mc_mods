package io.github.dirnei.mc.servus.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DuctShapesTest {

    @Test
    void radiusGrowsWithLineCount() {
        assertEquals(DuctShapes.THIN_RADIUS, DuctShapes.radius(0));
        assertEquals(DuctShapes.THIN_RADIUS, DuctShapes.radius(1));
        assertEquals(DuctShapes.THICK_RADIUS, DuctShapes.radius(2));
        assertEquals(DuctShapes.THICKEST_RADIUS, DuctShapes.radius(3));
        assertEquals(4, DuctShapes.THICKEST_RADIUS);
    }

    @Test
    void junctionGrowsByOnePixel() {
        int straight = 1 << DuctShapes.NORTH | 1 << DuctShapes.SOUTH;
        int bend = 1 << DuctShapes.NORTH | 1 << DuctShapes.EAST;
        assertFalse(DuctShapes.isJunction(straight));
        assertTrue(DuctShapes.isJunction(bend));
        assertEquals(DuctShapes.THICK_RADIUS, DuctShapes.centerRadius(2, straight));
        assertEquals(DuctShapes.THICK_RADIUS + 1, DuctShapes.centerRadius(2, bend));
        assertEquals(DuctShapes.THIN_RADIUS, DuctShapes.centerRadius(1, bend));
        assertEquals(DuctShapes.THICKEST_RADIUS, DuctShapes.centerRadius(3, straight));
        assertEquals(DuctShapes.THICKEST_RADIUS + 1, DuctShapes.centerRadius(3, bend));
    }

    @Test
    void centreIsCentred() {
        DuctShapes.Box thin = DuctShapes.center(2);
        assertEquals(new DuctShapes.Box(6, 6, 6, 10, 10, 10), thin);
        DuctShapes.Box thick = DuctShapes.center(3);
        assertEquals(new DuctShapes.Box(5, 5, 5, 11, 11, 11), thick);
        assertEquals(new DuctShapes.Box(4, 4, 4, 12, 12, 12), DuctShapes.center(DuctShapes.radius(3)));
    }

    @Test
    void armsReachTheBlockEdgeOnTheirSide() {
        assertEquals(new DuctShapes.Box(6, 6, 0, 10, 10, 6), DuctShapes.arm(DuctShapes.NORTH, 2));
        assertEquals(new DuctShapes.Box(6, 6, 10, 10, 10, 16), DuctShapes.arm(DuctShapes.SOUTH, 2));
        assertEquals(new DuctShapes.Box(0, 5, 5, 5, 11, 11), DuctShapes.arm(DuctShapes.WEST, 3));
        assertEquals(new DuctShapes.Box(11, 5, 5, 16, 11, 11), DuctShapes.arm(DuctShapes.EAST, 3));
        assertEquals(new DuctShapes.Box(6, 0, 6, 10, 6, 10), DuctShapes.arm(DuctShapes.DOWN, 2));
        assertEquals(new DuctShapes.Box(6, 10, 6, 10, 16, 10), DuctShapes.arm(DuctShapes.UP, 2));
    }

    @Test
    void boxContainment() {
        DuctShapes.Box north = DuctShapes.arm(DuctShapes.NORTH, 2);
        assertTrue(north.contains(8, 8, 1));
        assertFalse(north.contains(8, 8, 12));
        assertFalse(north.contains(3, 8, 1));
    }
}
