package io.github.dirnei.mc.servus.foundation.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SideTest {

    @Test
    void orderMatchesMinecraftDirection() {
        assertEquals(List.of(Side.DOWN, Side.UP, Side.NORTH, Side.SOUTH, Side.WEST, Side.EAST), Side.ALL);
    }

    @Test
    void oppositesPairUp() {
        for (Side side : Side.ALL) {
            assertEquals(side, side.opposite().opposite());
            assertEquals(-side.dx(), side.opposite().dx());
            assertEquals(-side.dy(), side.opposite().dy());
            assertEquals(-side.dz(), side.opposite().dz());
        }
    }

    @Test
    void offsetsAreUnitVectors() {
        for (Side side : Side.ALL) {
            assertEquals(1, Math.abs(side.dx()) + Math.abs(side.dy()) + Math.abs(side.dz()));
        }
        assertEquals(-1, Side.DOWN.dy());
        assertEquals(1, Side.UP.dy());
        assertEquals(-1, Side.NORTH.dz());
        assertEquals(1, Side.SOUTH.dz());
        assertEquals(-1, Side.WEST.dx());
        assertEquals(1, Side.EAST.dx());
    }
}
