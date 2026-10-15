package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import java.util.Set;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.Test;

class LiftUpgradesTest {

    /** Segments at y 3, 4 and 5, and a separate shaft at 7 and 8. */
    private static final IntPredicate COLUMN = Set.of(3, 4, 5, 7, 8)::contains;

    @Test
    void speedAndAccelGrowPerUpgrade() {
        assertEquals(0.1, LiftUpgrades.speed(0.1, 0.05, 0), 1e-9);
        assertEquals(0.3, LiftUpgrades.speed(0.1, 0.05, 4), 1e-9);
        assertEquals(0.015, LiftUpgrades.accel(0.005, 0.0025, 4), 1e-9);
        assertEquals(0.1, LiftUpgrades.speed(0.1, 0.05, -2), 1e-9);
    }

    @Test
    void bottomIsTheLowestSegmentBelow() {
        assertEquals(3, LiftUpgrades.bottom(5, COLUMN));
        assertEquals(3, LiftUpgrades.bottom(3, COLUMN));
        assertEquals(7, LiftUpgrades.bottom(8, COLUMN));
    }

    @Test
    void heirIsTheSegmentAbove() {
        assertEquals(OptionalInt.of(4), LiftUpgrades.heirOnRemove(3, COLUMN));
        assertTrue(LiftUpgrades.heirOnRemove(5, COLUMN).isEmpty());
        assertTrue(LiftUpgrades.heirOnRemove(8, COLUMN).isEmpty());
    }
}
