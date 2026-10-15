package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FieldDurationTest {

    @Test
    void underAMinute() {
        assertEquals("<1m", FieldDuration.of(59));
        assertEquals("<1m", FieldDuration.of(-5));
    }

    @Test
    void minutesHoursAndDays() {
        assertEquals("~45m", FieldDuration.of(45 * 60 + 30));
        assertEquals("~3h 20m", FieldDuration.of(3 * 3600 + 20 * 60));
        assertEquals("~2d 5h", FieldDuration.of(2 * 86400 + 5 * 3600 + 59));
    }
}
