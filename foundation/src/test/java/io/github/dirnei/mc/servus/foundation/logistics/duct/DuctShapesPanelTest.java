package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DuctShapesPanelTest {

    @Test
    void solidPanelIsOnePixelOnItsFace() {
        assertEquals(List.of(new DuctShapes.Box(0, 0, 0, 16, 1, 16)), DuctShapes.panel(DuctShapes.DOWN, false));
        assertEquals(List.of(new DuctShapes.Box(0, 15, 0, 16, 16, 16)), DuctShapes.panel(DuctShapes.UP, false));
        assertEquals(List.of(new DuctShapes.Box(0, 0, 0, 16, 16, 1)), DuctShapes.panel(DuctShapes.NORTH, false));
        assertEquals(List.of(new DuctShapes.Box(0, 0, 15, 16, 16, 16)), DuctShapes.panel(DuctShapes.SOUTH, false));
        assertEquals(List.of(new DuctShapes.Box(0, 0, 0, 1, 16, 16)), DuctShapes.panel(DuctShapes.WEST, false));
        assertEquals(List.of(new DuctShapes.Box(15, 0, 0, 16, 16, 16)), DuctShapes.panel(DuctShapes.EAST, false));
    }

    @Test
    void hollowPanelLeavesTheHoleOpen() {
        for (int side = 0; side < 6; side++) {
            List<DuctShapes.Box> boxes = DuctShapes.panel(side, true);
            assertEquals(4, boxes.size());
            DuctShapes.Box slab = DuctShapes.panel(side, false).get(0);
            double cx = (slab.x0() + slab.x1()) / 2;
            double cy = (slab.y0() + slab.y1()) / 2;
            double cz = (slab.z0() + slab.z1()) / 2;
            for (DuctShapes.Box box : boxes) {
                assertFalse(box.contains(cx, cy, cz), "the centre of side " + side + " is open");
            }
            // The thickest arm (radius 4) fits: its corner is not inside any frame box.
            for (DuctShapes.Box box : boxes) {
                assertFalse(inside(box, slab, 4.5, 4.5), "side " + side + " frame covers the arm corner");
                assertFalse(inside(box, slab, 11.5, 11.5), "side " + side + " frame covers the arm corner");
            }
            assertTrue(boxes.stream().anyMatch(box -> inside(box, slab, 2, 2)), "side " + side + " frame covers its corner");
        }
    }

    @Test
    void panelsStayInsideTheBlock() {
        for (int side = 0; side < 6; side++) {
            for (boolean hollow : new boolean[] {false, true}) {
                for (DuctShapes.Box box : DuctShapes.panel(side, hollow)) {
                    assertTrue(box.x0() >= 0 && box.y0() >= 0 && box.z0() >= 0 && box.x1() <= 16 && box.y1() <= 16 && box.z1() <= 16);
                    assertTrue(box.x0() < box.x1() && box.y0() < box.y1() && box.z0() < box.z1(), "box has volume");
                }
            }
        }
    }

    /** Whether the frame box holds the point (u, v) of the face, at the panel's depth. */
    private static boolean inside(DuctShapes.Box box, DuctShapes.Box slab, double u, double v) {
        double depth;
        if (slab.y1() - slab.y0() == 1) {
            depth = (slab.y0() + slab.y1()) / 2;
            return box.contains(u, depth, v);
        }
        if (slab.z1() - slab.z0() == 1) {
            depth = (slab.z0() + slab.z1()) / 2;
            return box.contains(u, v, depth);
        }
        depth = (slab.x0() + slab.x1()) / 2;
        return box.contains(depth, u, v);
    }
}
