package io.github.dirnei.mc.servus.shuttles.gantry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.gantry.GantryTanks.Tank;
import java.util.List;
import org.junit.jupiter.api.Test;

class GantryTanksTest {

    private static final int CAP = 16000;

    @Test
    void lockedTankRefusesOtherFluids() {
        Tank<String> sulfur = new Tank<>("sulfur", null, 0, CAP);
        assertFalse(sulfur.accepts("water"));
        assertTrue(sulfur.accepts("sulfur"));
        assertEquals(List.of(), GantryTanks.fillOrder(List.of(sulfur, sulfur), "water"));
    }

    @Test
    void anyTakesFirstFluidThenOnlyIt() {
        Tank<String> empty = new Tank<>(null, null, 0, CAP);
        assertTrue(empty.accepts("water"));
        assertTrue(empty.accepts("lava"));
        Tank<String> holding = new Tank<>(null, "water", 1000, CAP);
        assertTrue(holding.accepts("water"));
        assertFalse(holding.accepts("lava"));
    }

    @Test
    void fillOrderPrefersLockedThenHoldingThenEmpty() {
        Tank<String> emptyAny = new Tank<>(null, null, 0, CAP);
        Tank<String> holdingAny = new Tank<>(null, "water", 500, CAP);
        Tank<String> locked = new Tank<>("water", null, 0, CAP);
        assertEquals(List.of(1, 0), GantryTanks.fillOrder(List.of(emptyAny, locked), "water"));
        assertEquals(List.of(1, 0), GantryTanks.fillOrder(List.of(emptyAny, holdingAny), "water"));
        assertEquals(List.of(0, 1), GantryTanks.fillOrder(List.of(locked, holdingAny), "water"));
        Tank<String> full = new Tank<>("water", "water", CAP, CAP);
        assertEquals(List.of(1), GantryTanks.fillOrder(List.of(full, emptyAny), "water"));
    }

    @Test
    void lockChangeKeepsContentAndWarns() {
        Tank<String> tank = new Tank<>("mercury", "water", 5000, CAP);
        assertTrue(tank.warning());
        assertFalse(tank.accepts("water"));
        assertFalse(tank.accepts("mercury"));
        assertEquals(1, GantryTanks.firstWarning(List.of(new Tank<>(null, "water", 5000, CAP), tank)));
        assertEquals(-1, GantryTanks.firstWarning(List.of(new Tank<>("water", "water", 5000, CAP))));
    }
}
