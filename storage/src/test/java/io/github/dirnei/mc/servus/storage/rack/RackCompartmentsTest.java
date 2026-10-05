package io.github.dirnei.mc.servus.storage.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RackCompartmentsTest {

    private static final int SOUTH = 0;
    private static final int WEST = 1;
    private static final int NORTH = 2;
    private static final int EAST = 3;

    @Test
    void northFacingTopLeftIsEastSide() {
        assertEquals(0, RackCompartments.index(NORTH, 0.9, 0.9, 0.0));
        assertEquals(1, RackCompartments.index(NORTH, 0.1, 0.9, 0.0));
        assertEquals(2, RackCompartments.index(NORTH, 0.9, 0.1, 0.0));
        assertEquals(3, RackCompartments.index(NORTH, 0.1, 0.1, 0.0));
    }

    @Test
    void southFacingTopLeftIsWestSide() {
        assertEquals(0, RackCompartments.index(SOUTH, 0.1, 0.9, 1.0));
        assertEquals(1, RackCompartments.index(SOUTH, 0.9, 0.9, 1.0));
    }

    @Test
    void westFacingTopLeftIsNorthSide() {
        assertEquals(0, RackCompartments.index(WEST, 0.0, 0.9, 0.1));
        assertEquals(3, RackCompartments.index(WEST, 0.0, 0.1, 0.9));
    }

    @Test
    void eastFacingTopLeftIsSouthSide() {
        assertEquals(0, RackCompartments.index(EAST, 1.0, 0.9, 0.9));
        assertEquals(3, RackCompartments.index(EAST, 1.0, 0.1, 0.1));
    }

    @Test
    void centresMatchIndices() {
        assertEquals(0.25, RackCompartments.centreFromLeft(0));
        assertEquals(0.75, RackCompartments.centreFromLeft(3));
        assertEquals(0.75, RackCompartments.centreY(1));
        assertEquals(0.25, RackCompartments.centreY(2));
    }
}
