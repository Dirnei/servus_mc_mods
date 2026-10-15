package io.github.dirnei.mc.servus.alchemy.salamander;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SalamanderTemperTest {

    private static final double ALMOST_ONE = Math.nextDown(1.0);

    @Test
    void rejectsChanceOutsideZeroToOne() {
        assertThrows(IllegalArgumentException.class, () -> new SalamanderTemper(-0.1, 20));
        assertThrows(IllegalArgumentException.class, () -> new SalamanderTemper(1.1, 20));
        assertThrows(IllegalArgumentException.class, () -> new SalamanderTemper(Double.NaN, 20));
    }

    @Test
    void rejectsDigestBelowOneTick() {
        assertThrows(IllegalArgumentException.class, () -> new SalamanderTemper(0.5, 0));
    }

    @Test
    void rollBelowChanceBefriends() {
        SalamanderTemper temper = new SalamanderTemper(0.33, 20);
        assertTrue(temper.befriends(0.32));
        assertFalse(temper.befriends(0.33));
        assertFalse(temper.befriends(0.9));
    }

    @Test
    void chanceOneAlwaysBefriends() {
        SalamanderTemper temper = new SalamanderTemper(1, 20);
        assertTrue(temper.befriends(0));
        assertTrue(temper.befriends(ALMOST_ONE));
    }

    @Test
    void chanceZeroNever() {
        SalamanderTemper temper = new SalamanderTemper(0, 20);
        assertFalse(temper.befriends(0));
        assertFalse(temper.befriends(ALMOST_ONE));
    }
}
