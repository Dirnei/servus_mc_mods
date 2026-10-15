package io.github.dirnei.mc.servus.alchemy.salamander;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SalamanderSpawnTest {

    @Test
    void homeWithinRadiusCounts() {
        assertTrue(SalamanderSpawn.homeWithin(2, 0, -1, 4));
        assertTrue(SalamanderSpawn.homeWithin(-4, 4, 4, 4));
        assertTrue(SalamanderSpawn.homeWithin(0, 0, 0, 0));
    }

    @Test
    void homeOutsideRadiusDoesNot() {
        assertFalse(SalamanderSpawn.homeWithin(5, 0, 0, 4));
        assertFalse(SalamanderSpawn.homeWithin(0, -5, 0, 4));
        assertFalse(SalamanderSpawn.homeWithin(1, 0, 0, 0));
    }

    @Test
    void crowdAtMaxRefuses() {
        assertFalse(SalamanderSpawn.crowdAllows(3, 3));
        assertFalse(SalamanderSpawn.crowdAllows(4, 3));
    }

    @Test
    void crowdBelowMaxAllows() {
        assertTrue(SalamanderSpawn.crowdAllows(2, 3));
        assertTrue(SalamanderSpawn.crowdAllows(0, 1));
    }

    @Test
    void crowdMaxZeroRefusesAll() {
        assertFalse(SalamanderSpawn.crowdAllows(0, 0));
    }
}
