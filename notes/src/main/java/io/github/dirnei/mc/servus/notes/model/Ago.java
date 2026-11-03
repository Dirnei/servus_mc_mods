package io.github.dirnei.mc.servus.notes.model;

/** How long ago something happened, rounded the way the notebook says it ("just now", "5 min ago", "yesterday"). */
public record Ago(Unit unit, long amount) {

    public enum Unit {
        JUST_NOW,
        MINUTES,
        HOURS,
        YESTERDAY,
        DAYS
    }

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    /** The time from {@code thenMillis} to {@code nowMillis}; a time in the future counts as just now. */
    public static Ago between(long nowMillis, long thenMillis) {
        long elapsed = nowMillis - thenMillis;
        if (elapsed < MINUTE) {
            return new Ago(Unit.JUST_NOW, 0);
        }
        if (elapsed < HOUR) {
            return new Ago(Unit.MINUTES, elapsed / MINUTE);
        }
        if (elapsed < DAY) {
            return new Ago(Unit.HOURS, elapsed / HOUR);
        }
        if (elapsed < 2 * DAY) {
            return new Ago(Unit.YESTERDAY, 1);
        }
        return new Ago(Unit.DAYS, elapsed / DAY);
    }
}
