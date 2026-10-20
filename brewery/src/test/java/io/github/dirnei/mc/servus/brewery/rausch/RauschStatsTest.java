package io.github.dirnei.mc.servus.brewery.rausch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RauschStatsTest {

    private static final HangoverStats HANGOVER = RauschStats.DEFAULTS.hangover();

    @Test
    void unorderedThresholdsGiveTheHighestReachedLevel() {
        RauschStats stats = new RauschStats(1, 100, 10, Map.of(
                RauschLevel.TIPSY, LevelStats.mild(3, 0, 0),
                RauschLevel.MERRY, LevelStats.mild(1, 0, 0),
                RauschLevel.DRUNK, LevelStats.mild(6, 0, 0),
                RauschLevel.PLASTERED, LevelStats.mild(9, 0, 0)), HANGOVER);
        assertEquals(Optional.of(RauschLevel.MERRY), stats.levelAt(2));
        assertEquals(Optional.of(RauschLevel.TIPSY), stats.levelAt(4));
        assertEquals(Optional.of(RauschLevel.DRUNK), stats.levelAt(8));
        assertEquals(Optional.empty(), stats.levelAt(0));
    }

    @Test
    void missingLevelFallsBackToTheDefault() {
        RauschStats stats = new RauschStats(1, 100, 10, Map.of(RauschLevel.TIPSY, LevelStats.mild(1, 5, 5)), HANGOVER);
        assertEquals(5, stats.level(RauschLevel.TIPSY).luck());
        assertEquals(RauschStats.DEFAULTS.level(RauschLevel.PLASTERED), stats.level(RauschLevel.PLASTERED));
    }

    @Test
    void perDrinkOfTwoMakesMerryAtOnce() {
        RauschStats stats = new RauschStats(2, 100, 10, Map.of(), HANGOVER);
        Rausch rausch = Rausch.SOBER.drink(stats.perDrink(), stats, 0, 0);
        assertEquals(Optional.of(RauschLevel.MERRY), rausch.level(stats));
    }
}
