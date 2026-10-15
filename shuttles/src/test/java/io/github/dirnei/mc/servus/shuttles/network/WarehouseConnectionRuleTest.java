package io.github.dirnei.mc.servus.shuttles.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule.NodeKind;
import org.junit.jupiter.api.Test;

class WarehouseConnectionRuleTest {

    @Test
    void controllerLinksToRailsAndAControllerOnAnySide() {
        for (Side side : Side.ALL) {
            assertTrue(WarehouseConnectionRule.accepts(NodeKind.STOCK, side, NodeKind.STOCK));
            assertTrue(WarehouseConnectionRule.accepts(NodeKind.STOCK, side, NodeKind.RAIL));
            assertTrue(WarehouseConnectionRule.accepts(NodeKind.RAIL, side, NodeKind.STOCK));
        }
    }

    @Test
    void controllerNeverLinksStraightToAnElevator() {
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
    void stationLinksOnlyToRails() {
        for (Side side : Side.ALL) {
            assertTrue(WarehouseConnectionRule.accepts(NodeKind.STATION, side, NodeKind.RAIL));
            assertTrue(WarehouseConnectionRule.accepts(NodeKind.RAIL, side, NodeKind.STATION));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.STATION, side, NodeKind.STOCK));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.STATION, side, NodeKind.STATION));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.STATION, side, NodeKind.GARAGE));
        }
    }

    @Test
    void controllerAndGarageNeverLinkToStations() {
        for (Side side : Side.ALL) {
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.STOCK, side, NodeKind.STATION));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.GARAGE, side, NodeKind.STATION));
        }
    }

    @Test
    void stationNeverLinksToAnElevator() {
        for (Side side : Side.ALL) {
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.STATION, side, NodeKind.ELEVATOR));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.ELEVATOR, side, NodeKind.STATION));
        }
    }

    @Test
    void shuttleServedKindsAreRailOnly() {
        for (NodeKind kind : new NodeKind[] {NodeKind.STATION, NodeKind.GARAGE, NodeKind.RACK}) {
            assertTrue(WarehouseConnectionRule.railOnly(kind));
            for (Side side : Side.ALL) {
                assertTrue(WarehouseConnectionRule.accepts(kind, side, NodeKind.RAIL));
                assertFalse(WarehouseConnectionRule.accepts(kind, side, NodeKind.STOCK));
                assertFalse(WarehouseConnectionRule.accepts(NodeKind.STOCK, side, kind));
                assertFalse(WarehouseConnectionRule.accepts(kind, side, NodeKind.ELEVATOR));
            }
        }
        assertFalse(WarehouseConnectionRule.railOnly(NodeKind.STOCK));
        for (Side side : Side.ALL) {
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.GARAGE, side, NodeKind.GARAGE));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.GARAGE, side, NodeKind.STATION));
        }
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

    @Test
    void racksLinkOnlyToRailsAndRacks() {
        for (Side side : Side.ALL) {
            assertTrue(WarehouseConnectionRule.accepts(NodeKind.RACK, side, NodeKind.RACK));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.RACK, side, NodeKind.STOCK));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.STOCK, side, NodeKind.RACK));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.STATION, side, NodeKind.RACK));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.GARAGE, side, NodeKind.RACK));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.ELEVATOR, side, NodeKind.RACK));
            assertFalse(WarehouseConnectionRule.accepts(NodeKind.RACK, side, NodeKind.ELEVATOR));
        }
    }

    @Test
    void railsTakeRacks() {
        assertTrue(WarehouseConnectionRule.accepts(NodeKind.RAIL, Side.NORTH, NodeKind.RACK));
        assertTrue(WarehouseConnectionRule.accepts(NodeKind.RACK, Side.SOUTH, NodeKind.RAIL));
    }

    @Test
    void controllerNeverLinksToShuttleServedBlocks() {
        for (Side side : Side.ALL) {
            for (NodeKind kind : new NodeKind[] {NodeKind.RACK, NodeKind.STATION, NodeKind.GARAGE, NodeKind.ELEVATOR}) {
                assertFalse(WarehouseConnectionRule.accepts(NodeKind.STOCK, side, kind), "controller to " + kind);
                assertFalse(WarehouseConnectionRule.accepts(kind, side, NodeKind.STOCK), kind + " to controller");
            }
        }
    }
}
