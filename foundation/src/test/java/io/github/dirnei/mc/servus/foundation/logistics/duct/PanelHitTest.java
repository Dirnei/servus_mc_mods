package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PanelHitTest {

    private static final int NORTH = 1 << DuctShapes.NORTH;
    private static final int UP = 1 << DuctShapes.UP;

    @Test
    void hitInASlabPicksThatFace() {
        assertEquals(DuctShapes.NORTH, PanelHit.face(8, 3, 0, DuctShapes.NORTH, NORTH, 0));
        assertEquals(DuctShapes.NORTH, PanelHit.face(8, 3, 1, DuctShapes.SOUTH, NORTH, 0), "the inner face of the panel");
    }

    @Test
    void clickedFaceWinsOnASharedEdge() {
        // The edge where the north and the top slab meet.
        assertEquals(DuctShapes.UP, PanelHit.face(8, 16, 0.5, DuctShapes.UP, NORTH | UP, 0));
        assertEquals(DuctShapes.NORTH, PanelHit.face(8, 15.5, 0, DuctShapes.NORTH, NORTH | UP, 0));
    }

    @Test
    void noPanelGivesNone() {
        assertEquals(PanelHit.NONE, PanelHit.face(8, 8, 8, DuctShapes.NORTH, NORTH, 0), "the duct centre");
        assertEquals(PanelHit.NONE, PanelHit.face(8, 8, 16, DuctShapes.SOUTH, NORTH, 0), "the south face has no panel");
    }

    @Test
    void holeOfAHollowPanelIsNotThePanel() {
        assertEquals(PanelHit.NONE, PanelHit.face(8, 8, 0, DuctShapes.NORTH, 0, NORTH));
        assertEquals(DuctShapes.NORTH, PanelHit.face(2, 8, 0, DuctShapes.NORTH, 0, NORTH), "the frame");
    }
}
