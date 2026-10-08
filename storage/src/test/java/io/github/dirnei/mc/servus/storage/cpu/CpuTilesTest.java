package io.github.dirnei.mc.servus.storage.cpu;

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
    void eastFaceLeftIsSouth() {
        assertEquals("tile_bl", CpuTiles.tile(CpuShape.part(2, 0, 2), Side.EAST));
        assertEquals("tile_r", CpuTiles.tile(CpuShape.part(2, 1, 0), Side.EAST));
    }
}
