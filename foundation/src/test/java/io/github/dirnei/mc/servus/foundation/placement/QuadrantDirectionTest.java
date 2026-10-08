package io.github.dirnei.mc.servus.foundation.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.placement.QuadrantDirection.Edge;
import org.junit.jupiter.api.Test;

class QuadrantDirectionTest {

    @Test
    void fourEdges() {
        assertEquals(Edge.NORTH, QuadrantDirection.nearestEdge(0.5, 0.1));
        assertEquals(Edge.EAST, QuadrantDirection.nearestEdge(0.9, 0.5));
        assertEquals(Edge.SOUTH, QuadrantDirection.nearestEdge(0.5, 0.9));
        assertEquals(Edge.WEST, QuadrantDirection.nearestEdge(0.1, 0.5));
    }

    @Test
    void offCentreHitsPickTheCloserEdge() {
        assertEquals(Edge.EAST, QuadrantDirection.nearestEdge(0.8, 0.3));
        assertEquals(Edge.NORTH, QuadrantDirection.nearestEdge(0.6, 0.2));
    }

    @Test
    void tiesAreDeterministic() {
        assertEquals(Edge.NORTH, QuadrantDirection.nearestEdge(0.5, 0.5));
        assertEquals(Edge.NORTH, QuadrantDirection.nearestEdge(0.75, 0.25));
        assertEquals(Edge.EAST, QuadrantDirection.nearestEdge(0.75, 0.75));
        assertEquals(Edge.SOUTH, QuadrantDirection.nearestEdge(0.25, 0.75));
        assertEquals(Edge.NORTH, QuadrantDirection.nearestEdge(0.25, 0.25));
    }
}
