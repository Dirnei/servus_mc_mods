package io.github.dirnei.mc.servus.storage.crate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CrateCompartmentsTest {

    private static final int SOUTH = 0;
    private static final int WEST = 1;
    private static final int NORTH = 2;
    private static final int EAST = 3;

    @Test
    void singleCompartmentIsAlwaysZero() {
        assertEquals(0, CrateCompartments.index(1, NORTH, 0.1, 0.1, 0.0, true));
        assertEquals(0, CrateCompartments.index(1, SOUTH, 0.9, 0.9, 1.0, false));
    }

    @Test
    void sideFacesMeanTheFirstCompartment() {
        assertEquals(0, CrateCompartments.index(4, NORTH, 0.1, 0.1, 0.5, false));
        assertEquals(0, CrateCompartments.index(2, EAST, 0.5, 0.5, 0.9, false));
    }

    @Test
    void doubleCrateSplitsLeftAndRight() {
        assertEquals(0, CrateCompartments.index(2, NORTH, 0.9, 0.1, 0.0, true));
        assertEquals(1, CrateCompartments.index(2, NORTH, 0.1, 0.9, 0.0, true));
        assertEquals(0, CrateCompartments.index(2, SOUTH, 0.1, 0.5, 1.0, true));
        assertEquals(1, CrateCompartments.index(2, SOUTH, 0.9, 0.5, 1.0, true));
    }

    @Test
    void quadNorthFacingTopLeftIsEastSide() {
        assertEquals(0, CrateCompartments.index(4, NORTH, 0.9, 0.9, 0.0, true));
        assertEquals(1, CrateCompartments.index(4, NORTH, 0.1, 0.9, 0.0, true));
        assertEquals(2, CrateCompartments.index(4, NORTH, 0.9, 0.1, 0.0, true));
        assertEquals(3, CrateCompartments.index(4, NORTH, 0.1, 0.1, 0.0, true));
    }

    @Test
    void quadOtherFacings() {
        assertEquals(0, CrateCompartments.index(4, SOUTH, 0.1, 0.9, 1.0, true));
        assertEquals(1, CrateCompartments.index(4, SOUTH, 0.9, 0.9, 1.0, true));
        assertEquals(0, CrateCompartments.index(4, WEST, 0.0, 0.9, 0.1, true));
        assertEquals(3, CrateCompartments.index(4, WEST, 0.0, 0.1, 0.9, true));
        assertEquals(0, CrateCompartments.index(4, EAST, 1.0, 0.9, 0.9, true));
        assertEquals(3, CrateCompartments.index(4, EAST, 1.0, 0.1, 0.1, true));
    }

    @Test
    void centresFollowTheLayout() {
        assertEquals(0.5, CrateCompartments.centreFromLeft(1, 0));
        assertEquals(0.5, CrateCompartments.centreY(1, 0));
        assertEquals(0.25, CrateCompartments.centreFromLeft(2, 0));
        assertEquals(0.75, CrateCompartments.centreFromLeft(2, 1));
        assertEquals(0.5, CrateCompartments.centreY(2, 1));
        assertEquals(0.25, CrateCompartments.centreFromLeft(4, 0));
        assertEquals(0.75, CrateCompartments.centreFromLeft(4, 3));
        assertEquals(0.75, CrateCompartments.centreY(4, 1));
        assertEquals(0.25, CrateCompartments.centreY(4, 2));
    }
}
