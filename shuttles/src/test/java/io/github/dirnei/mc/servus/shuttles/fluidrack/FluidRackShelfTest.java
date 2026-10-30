package io.github.dirnei.mc.servus.shuttles.fluidrack;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FluidRackShelfTest {

    @Test
    void portOfOneTwoAndThree() {
        assertEquals(0, FluidRackShelf.portIndex(1));
        assertEquals(0, FluidRackShelf.portIndex(2));
        assertEquals(1, FluidRackShelf.portIndex(3));
    }

    @Test
    void sharesSplitEvenlyRemainderLeft() {
        assertArrayEquals(new int[] {10000, 10000, 10000}, FluidRackShelf.shares(30000, 3));
        assertArrayEquals(new int[] {4, 3, 3}, FluidRackShelf.shares(10, 3));
        assertArrayEquals(new int[] {3, 3, 2}, FluidRackShelf.shares(8, 3));
        assertArrayEquals(new int[] {1001, 1000}, FluidRackShelf.shares(2001, 2));
        assertArrayEquals(new int[] {500}, FluidRackShelf.shares(500, 1));
    }

    @Test
    void zeroTotalGivesZeroShares() {
        assertArrayEquals(new int[] {0, 0, 0}, FluidRackShelf.shares(0, 3));
        assertArrayEquals(new int[] {}, FluidRackShelf.shares(100, 0));
    }

    @Test
    void differentFluidCannotJoin() {
        assertFalse(FluidRackShelf.canJoin("water", "lava"));
        assertTrue(FluidRackShelf.canJoin("water", "water"));
        assertTrue(FluidRackShelf.canJoin(null, "lava"));
        assertTrue(FluidRackShelf.canJoin("water", null));
        assertTrue(FluidRackShelf.canJoin(null, null));
    }
}
