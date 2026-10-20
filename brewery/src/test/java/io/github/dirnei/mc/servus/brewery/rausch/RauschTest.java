package io.github.dirnei.mc.servus.brewery.rausch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RauschTest {

    private static final RauschStats STATS = RauschStats.DEFAULTS;
    private static final int DECAY = STATS.decayTicks();
    private static final long DAY = 7;
    private static final long NOW = 170_000;

    private static Rausch afterDrinks(int drinks) {
        Rausch rausch = Rausch.SOBER;
        for (int i = 0; i < drinks; i++) {
            rausch = rausch.drink(STATS.perDrink(), STATS, DAY, NOW);
        }
        return rausch;
    }

    @Test
    void drinkingAddsAndCaps() {
        assertEquals(1, afterDrinks(1).points());
        assertEquals(3, afterDrinks(3).points());
        assertEquals(STATS.max(), afterDrinks(STATS.max() + 3).points());
    }

    @Test
    void onePointWearsOffPerDecayPeriod() {
        Rausch rausch = afterDrinks(3);
        assertEquals(3, rausch.pass(DECAY - 1, STATS).points());
        assertEquals(2, rausch.pass(DECAY, STATS).points());
        assertEquals(2, rausch.pass(DECAY - 1, STATS).pass(DECAY, STATS).points());
        assertEquals(1, rausch.pass(DECAY - 1, STATS).pass(DECAY + 1, STATS).points());
        assertEquals(DECAY - 1, rausch.pass(DECAY - 1, STATS).pass(DECAY, STATS).progress());
    }

    @Test
    void neverBelowZero() {
        Rausch rausch = afterDrinks(2).pass(DECAY * 10, STATS);
        assertEquals(0, rausch.points());
        assertEquals(0, rausch.progress());
        assertEquals(Rausch.SOBER, Rausch.SOBER.pass(DECAY * 3, STATS));
    }

    @Test
    void levelsAtEachThreshold() {
        assertEquals(Optional.empty(), afterDrinks(0).level(STATS));
        assertEquals(Optional.of(RauschLevel.TIPSY), afterDrinks(1).level(STATS));
        assertEquals(Optional.of(RauschLevel.MERRY), afterDrinks(2).level(STATS));
        assertEquals(Optional.of(RauschLevel.MERRY), afterDrinks(3).level(STATS));
        assertEquals(Optional.of(RauschLevel.DRUNK), afterDrinks(4).level(STATS));
        assertEquals(Optional.of(RauschLevel.PLASTERED), afterDrinks(5).level(STATS));
        assertEquals(Optional.of(RauschLevel.PLASTERED), afterDrinks(8).level(STATS));
    }

    @Test
    void ticksAtLevel() {
        assertEquals(0, Rausch.SOBER.ticksAtLevel(STATS));
        assertEquals(DECAY, afterDrinks(1).ticksAtLevel(STATS));
        assertEquals(2 * DECAY, afterDrinks(3).ticksAtLevel(STATS));
        assertEquals(2 * DECAY - 100, afterDrinks(3).pass(100, STATS).ticksAtLevel(STATS));
    }

    @Test
    void hangoverDueAtDrunkNotMerry() {
        assertEquals(-1, afterDrinks(3).dueDay());
        assertEquals(DAY, afterDrinks(4).dueDay());
        Rausch later = afterDrinks(4).drink(1, STATS, DAY + 1, NOW + 24000);
        assertEquals(DAY, later.dueDay());
    }

    @Test
    void morningOnTheSameDayDoesNothing() {
        Rausch drunk = afterDrinks(4);
        assertEquals(drunk, drunk.morning(DAY, NOW + 100, STATS));
        assertEquals(afterDrinks(3), afterDrinks(3).morning(DAY + 1, NOW + 100, STATS));
    }

    @Test
    void morningOnTheNextDayStartsTheHangover() {
        Rausch hungover = afterDrinks(4).pass(DECAY * 4, STATS).morning(DAY + 1, NOW, STATS);
        assertEquals(0, hungover.points());
        assertEquals(-1, hungover.dueDay());
        assertTrue(hungover.hungover(NOW));
        assertEquals(STATS.hangover().duration(), hungover.hangoverLeft(NOW));
        assertFalse(hungover.hungover(NOW + STATS.hangover().duration()));
        assertEquals(0, hungover.hangoverLeft(NOW + STATS.hangover().duration() + 5));
    }

    @Test
    void hairOfTheDogEndsTheHangover() {
        Rausch hungover = afterDrinks(5).morning(DAY + 1, NOW, STATS);
        Rausch cured = hungover.drink(STATS.perDrink(), STATS, DAY + 1, NOW + 10);
        assertFalse(cured.hungover(NOW + 10));
        assertEquals(STATS.hangover().hairOfTheDog(), cured.points());
        assertEquals(Optional.of(RauschLevel.TIPSY), cured.level(STATS));
        assertEquals(-1, cured.dueDay());
    }

    @Test
    void drinkingAfterTheHangoverAddsNormally() {
        long end = NOW + STATS.hangover().duration();
        Rausch rausch = afterDrinks(4).morning(DAY + 1, NOW, STATS).drink(1, STATS, DAY + 1, end).drink(1, STATS, DAY + 1, end);
        assertEquals(2, rausch.points());
    }
}
