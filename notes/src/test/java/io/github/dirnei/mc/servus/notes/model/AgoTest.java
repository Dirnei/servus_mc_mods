package io.github.dirnei.mc.servus.notes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.notes.model.Ago.Unit;
import org.junit.jupiter.api.Test;

class AgoTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    @Test
    void underAMinuteIsJustNow() {
        assertEquals(new Ago(Unit.JUST_NOW, 0), Ago.between(NOW, NOW - 59_000));
    }

    @Test
    void futureIsJustNow() {
        assertEquals(new Ago(Unit.JUST_NOW, 0), Ago.between(NOW, NOW + 5 * MINUTE));
    }

    @Test
    void minutesRoundDown() {
        assertEquals(new Ago(Unit.MINUTES, 5), Ago.between(NOW, NOW - 5 * MINUTE - 59_000));
    }

    @Test
    void hours() {
        assertEquals(new Ago(Unit.HOURS, 2), Ago.between(NOW, NOW - 2 * HOUR - 10 * MINUTE));
    }

    @Test
    void oneToTwoDaysIsYesterday() {
        assertEquals(new Ago(Unit.YESTERDAY, 1), Ago.between(NOW, NOW - DAY - 3 * HOUR));
    }

    @Test
    void days() {
        assertEquals(new Ago(Unit.DAYS, 3), Ago.between(NOW, NOW - 3 * DAY - HOUR));
    }
}
