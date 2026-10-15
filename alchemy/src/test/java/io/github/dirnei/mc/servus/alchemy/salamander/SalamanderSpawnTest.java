package io.github.dirnei.mc.servus.alchemy.salamander;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SalamanderSpawnTest {

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
