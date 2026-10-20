package io.github.dirnei.mc.servus.brewery.malting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GerminationTest {

    private static final long START = 1000;
    private static final long TIME = 24000;
    private static final long BONUS = 2400;
    private static final long COOLDOWN = 1200;

    @Test
    void notDoneBeforeTheTime() {
        Germination batch = Germination.start(START);
        assertFalse(batch.isDone(START + TIME - 1, TIME));
        assertEquals(1, batch.remaining(START + TIME - 1, TIME));
    }

    @Test
    void doneAtTheTime() {
        assertTrue(Germination.start(START).isDone(START + TIME, TIME));
    }

    @Test
    void remainingNeverBelowZero() {
        assertEquals(0, Germination.start(START).remaining(START + TIME * 3, TIME));
    }

    @Test
    void aTurnAddsTheBonus() {
        Germination turned = Germination.start(START).turn(START + 10, COOLDOWN, BONUS).orElseThrow();
        assertEquals(10 + BONUS, turned.elapsed(START + 10));
        assertEquals(TIME - 10 - BONUS, turned.remaining(START + 10, TIME));
    }

    @Test
    void aFreshBatchCanBeTurnedAtOnce() {
        assertTrue(Germination.start(START).canTurn(START, COOLDOWN));
    }

    @Test
    void aSecondTurnWithinTheCooldownIsRefused() {
        Germination turned = Germination.start(START).turn(START, COOLDOWN, BONUS).orElseThrow();
        assertTrue(turned.turn(START + COOLDOWN - 1, COOLDOWN, BONUS).isEmpty());
    }

    @Test
    void aTurnAfterTheCooldownIsAllowed() {
        Germination turned = Germination.start(START).turn(START, COOLDOWN, BONUS).orElseThrow();
        Germination again = turned.turn(START + COOLDOWN, COOLDOWN, BONUS).orElseThrow();
        assertEquals(2 * BONUS, again.bonus());
        assertEquals(START + COOLDOWN, again.lastTurn());
    }

    @Test
    void bonusPastTheTimeMakesItDone() {
        Germination turned = Germination.start(START).turn(START, COOLDOWN, TIME).orElseThrow();
        assertTrue(turned.isDone(START, TIME));
    }
}
