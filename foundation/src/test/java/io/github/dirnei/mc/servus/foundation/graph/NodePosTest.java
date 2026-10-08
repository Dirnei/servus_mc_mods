package io.github.dirnei.mc.servus.foundation.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NodePosTest {

    @Test
    void relativeMovesOneStep() {
        NodePos origin = new NodePos(1, 2, 3);
        assertEquals(new NodePos(1, 3, 3), origin.relative(Side.UP));
        assertEquals(new NodePos(1, 1, 3), origin.relative(Side.DOWN));
        assertEquals(new NodePos(1, 2, 2), origin.relative(Side.NORTH));
        assertEquals(new NodePos(1, 2, 4), origin.relative(Side.SOUTH));
        assertEquals(new NodePos(0, 2, 3), origin.relative(Side.WEST));
        assertEquals(new NodePos(2, 2, 3), origin.relative(Side.EAST));
    }

    @Test
    void relativeAndBackIsIdentity() {
        NodePos origin = new NodePos(-4, 70, 12);
        for (Side side : Side.ALL) {
            assertEquals(origin, origin.relative(side).relative(side.opposite()));
        }
    }
}
