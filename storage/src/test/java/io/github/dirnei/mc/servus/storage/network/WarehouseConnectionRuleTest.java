package io.github.dirnei.mc.servus.storage.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.lib.graph.Side;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule.NodeKind;
import org.junit.jupiter.api.Test;

class WarehouseConnectionRuleTest {

    @Test
    void stockLinksToStockAndRailsOnAnySide() {
        for (Side side : Side.ALL) {
            assertTrue(WarehouseConnectionRule.accepts(NodeKind.STOCK, side, NodeKind.STOCK));
            assertTrue(WarehouseConnectionRule.accepts(NodeKind.STOCK, side, NodeKind.RAIL));
            assertTrue(WarehouseConnectionRule.accepts(NodeKind.RAIL, side, NodeKind.STOCK));
        }
    }

    @Test
    void stockNeverLinksStraightToAnElevator() {
        for (Side side : Side.ALL) {
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.STOCK, side, NodeKind.ELEVATOR));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.ELEVATOR, side, NodeKind.STOCK));
        }
    }

    @Test
    void railsLinkSidewaysOnly() {
        assertTrue(WarehouseConnectionRule.accepts(NodeKind.RAIL, Side.NORTH, NodeKind.RAIL));
        assertTrue(WarehouseConnectionRule.accepts(NodeKind.RAIL, Side.EAST, NodeKind.ELEVATOR));
        assertFalse(WarehouseConnectionRule.accepts(NodeKind.RAIL, Side.UP, NodeKind.RAIL));
        assertFalse(WarehouseConnectionRule.accepts(NodeKind.RAIL, Side.DOWN, NodeKind.ELEVATOR));
    }

    @Test
    void elevatorsStackVerticallyAndTakeRailsSideways() {
        assertTrue(WarehouseConnectionRule.accepts(NodeKind.ELEVATOR, Side.UP, NodeKind.ELEVATOR));
        assertTrue(WarehouseConnectionRule.accepts(NodeKind.ELEVATOR, Side.DOWN, NodeKind.ELEVATOR));
        assertFalse(WarehouseConnectionRule.accepts(NodeKind.ELEVATOR, Side.WEST, NodeKind.ELEVATOR));
        assertTrue(WarehouseConnectionRule.accepts(NodeKind.ELEVATOR, Side.WEST, NodeKind.RAIL));
        assertFalse(WarehouseConnectionRule.accepts(NodeKind.ELEVATOR, Side.UP, NodeKind.RAIL));
    }

    @Test
    void ruleIsSymmetric() {
        for (NodeKind a : NodeKind.values()) {
            for (NodeKind b : NodeKind.values()) {
                for (Side side : Side.ALL) {
                    Side back = switch (side) {
                        case UP -> Side.DOWN;
                        case DOWN -> Side.UP;
                        case NORTH -> Side.SOUTH;
                        case SOUTH -> Side.NORTH;
                        case WEST -> Side.EAST;
                        case EAST -> Side.WEST;
                    };
                    assertEquals(WarehouseConnectionRule.accepts(a, side, b), WarehouseConnectionRule.accepts(b, back, a),
                            a + " " + side + " " + b);
                }
            }
        }
    }
}
