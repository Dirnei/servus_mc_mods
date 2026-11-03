package io.github.dirnei.mc.servus.notes.model;

import java.util.Objects;

/** Who ticked a task, on which in-game day and at which real time (milliseconds since the epoch, server clock). */
public record DoneRecord(Person by, long day, long epochMillis) {

    public DoneRecord {
        Objects.requireNonNull(by, "by");
    }
}
