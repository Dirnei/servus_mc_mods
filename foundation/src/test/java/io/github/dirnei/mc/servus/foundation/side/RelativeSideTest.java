package io.github.dirnei.mc.servus.foundation.side;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import org.junit.jupiter.api.Test;

class RelativeSideTest {

    @Test
    void facingNorth() {
        assertEquals(Side.NORTH, RelativeSide.FRONT.toSide(Side.NORTH));
        assertEquals(Side.SOUTH, RelativeSide.BACK.toSide(Side.NORTH));
        assertEquals(Side.EAST, RelativeSide.LEFT.toSide(Side.NORTH));
        assertEquals(Side.WEST, RelativeSide.RIGHT.toSide(Side.NORTH));
        assertEquals(Side.UP, RelativeSide.TOP.toSide(Side.NORTH));
        assertEquals(Side.DOWN, RelativeSide.BOTTOM.toSide(Side.NORTH));
    }

    @Test
    void facingEast() {
        assertEquals(Side.EAST, RelativeSide.FRONT.toSide(Side.EAST));
        assertEquals(Side.WEST, RelativeSide.BACK.toSide(Side.EAST));
        assertEquals(Side.SOUTH, RelativeSide.LEFT.toSide(Side.EAST));
        assertEquals(Side.NORTH, RelativeSide.RIGHT.toSide(Side.EAST));
    }

    @Test
    void roundTripForEveryFacing() {
        for (Side facing : new Side[] {Side.NORTH, Side.EAST, Side.SOUTH, Side.WEST}) {
            for (RelativeSide relative : RelativeSide.values()) {
                assertEquals(relative, RelativeSide.fromSide(facing, relative.toSide(facing)));
            }
        }
    }
}
