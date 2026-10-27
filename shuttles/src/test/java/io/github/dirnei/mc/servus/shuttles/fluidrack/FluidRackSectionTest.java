package io.github.dirnei.mc.servus.shuttles.fluidrack;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FluidRackSectionTest {

    @Test
    void portOfOneTwoAndThree() {
        assertEquals(0, FluidRackSection.portIndex(1));
        assertEquals(0, FluidRackSection.portIndex(2));
        assertEquals(1, FluidRackSection.portIndex(3));
    }

    @Test
    void sharesSplitEvenlyRemainderLeft() {
        assertArrayEquals(new int[] {10000, 10000, 10000}, FluidRackSection.shares(30000, 3));
        assertArrayEquals(new int[] {4, 3, 3}, FluidRackSection.shares(10, 3));
        assertArrayEquals(new int[] {3, 3, 2}, FluidRackSection.shares(8, 3));
        assertArrayEquals(new int[] {1001, 1000}, FluidRackSection.shares(2001, 2));
        assertArrayEquals(new int[] {500}, FluidRackSection.shares(500, 1));
    }

    @Test
    void zeroTotalGivesZeroShares() {
        assertArrayEquals(new int[] {0, 0, 0}, FluidRackSection.shares(0, 3));
        assertArrayEquals(new int[] {}, FluidRackSection.shares(100, 0));
    }

    @Test
    void differentFluidCannotJoin() {
        assertFalse(FluidRackSection.canJoin("water", "lava"));
        assertTrue(FluidRackSection.canJoin("water", "water"));
        assertTrue(FluidRackSection.canJoin(null, "lava"));
        assertTrue(FluidRackSection.canJoin("water", null));
        assertTrue(FluidRackSection.canJoin(null, null));
    }
}
