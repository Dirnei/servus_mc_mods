package io.github.dirnei.mc.servus.shuttles.cpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import org.junit.jupiter.api.Test;

class CpuTilesTest {

    @Test
    void lowCornerOnTheNorthFaceIsBottomRight() {
        // Seen from the north, west is on the right.
        assertEquals("tile_br", CpuTiles.tile(CpuShape.part(0, 0, 0), Side.NORTH));
    }

    @Test
    void innerFacesShowTheCentre() {
        assertEquals("tile_c", CpuTiles.tile(CpuShape.part(0, 0, 0), Side.SOUTH));
        assertEquals("tile_c", CpuTiles.tile(CpuShape.part(1, 1, 1), Side.UP));
    }

    @Test
    void middleOfAFaceShowsOnlyLights() {
        assertEquals("tile_c", CpuTiles.tile(CpuShape.part(1, 1, 0), Side.NORTH));
    }

    @Test
    void topEdgeOfTheNorthFace() {
        assertEquals("tile_t", CpuTiles.tile(CpuShape.part(1, 2, 0), Side.NORTH));
    }

    @Test
    void upFaceTopIsNorth() {
        assertEquals("tile_tl", CpuTiles.tile(CpuShape.part(0, 2, 0), Side.UP));
        assertEquals("tile_br", CpuTiles.tile(CpuShape.part(2, 2, 2), Side.UP));
    }

    @Test
    void downFaceTopIsSouth() {
        assertEquals("tile_bl", CpuTiles.tile(CpuShape.part(0, 0, 0), Side.DOWN));
    }

    @Test
    void smallestBoxHasNoCentreTileOutside() {
        CpuShape.Vec size = new CpuShape.Vec(2, 2, 2);
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    int part = CpuBox.part(new CpuShape.Vec(x, y, z), size);
                    CpuShape.Vec place = CpuShape.offset(part);
                    for (Side face : Side.values()) {
                        if (CpuTiles.onBoundary(place, face)) {
                            String tile = CpuTiles.tile(part, face);
                            org.junit.jupiter.api.Assertions.assertNotEquals("tile_c", tile,
                                    "block " + x + "," + y + "," + z + " face " + face);
                            org.junit.jupiter.api.Assertions.assertEquals(7, tile.length(),
                                    "corner tile expected for " + x + "," + y + "," + z + " face " + face);
                        }
                    }
                }
            }
        }
    }

    @Test
    void eastFaceLeftIsSouth() {
        assertEquals("tile_bl", CpuTiles.tile(CpuShape.part(2, 0, 2), Side.EAST));
        assertEquals("tile_r", CpuTiles.tile(CpuShape.part(2, 1, 0), Side.EAST));
    }
}
