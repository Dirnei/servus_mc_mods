package io.github.dirnei.mc.servus.alchemy.salamander;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
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

    @Test
    void spawnDefaultsWithoutObject() {
        SalamanderTemper.Spawn spawn = new SalamanderTemper(0.5, 20).spawn();
        assertEquals(SalamanderTemper.Spawn.DEFAULT, spawn);
        assertEquals(4, spawn.homeRadius());
        assertEquals(32, spawn.crowdRadius());
        assertEquals(3, spawn.crowdMax());
        assertTrue(spawn.biomeAllows("minecraft:crimson_forest", ALMOST_ONE), "a biome without an entry always passes");
        assertTrue(spawn.biomeAllows("minecraft:warped_forest", 0.49));
        assertFalse(spawn.biomeAllows("minecraft:warped_forest", 0.5));
    }

    @Test
    void rejectsNegativeRadius() {
        assertThrows(IllegalArgumentException.class, () -> new SalamanderTemper.Spawn(-1, 32, 3, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new SalamanderTemper.Spawn(4, -1, 3, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new SalamanderTemper.Spawn(5, 32, 3, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new SalamanderTemper.Spawn(4, 32, -1, Map.of()));
    }

    @Test
    void rejectsBiomeChanceAboveOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new SalamanderTemper.Spawn(4, 32, 3, Map.of("minecraft:warped_forest", 1.5)));
        assertThrows(IllegalArgumentException.class,
                () -> new SalamanderTemper.Spawn(4, 32, 3, Map.of("minecraft:warped_forest", -0.1)));
    }
}
