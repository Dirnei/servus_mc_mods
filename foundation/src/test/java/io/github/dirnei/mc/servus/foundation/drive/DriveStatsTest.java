package io.github.dirnei.mc.servus.foundation.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class DriveStatsTest {

    private static DriveStats.Partial partial(Integer workPerTick, Integer workPerTurn, Float exhaustionPerTurn) {
        return new DriveStats.Partial(Optional.ofNullable(workPerTick), Optional.ofNullable(workPerTurn), Optional.ofNullable(exhaustionPerTurn));
    }

    @Test
    void fileOverridesTheDefault() {
        DriveStats stats = partial(null, 50, null).over(DriveStats.HAND_CRANK);
        assertEquals(50, stats.workPerTurn());
        assertEquals(DriveStats.HAND_CRANK.exhaustionPerTurn(), stats.exhaustionPerTurn(), 1e-6);
    }

    @Test
    void emptyFileKeepsTheDefaults() {
        assertEquals(DriveStats.ELECTRIC_MOTOR, partial(null, null, null).over(DriveStats.ELECTRIC_MOTOR));
    }

    @Test
    void negativeValuesCountAsZero() {
        DriveStats stats = partial(-3, null, -1F).over(DriveStats.ELECTRIC_MOTOR);
        assertEquals(0, stats.workPerTick());
        assertEquals(0, stats.exhaustionPerTurn(), 1e-6);
    }
}
