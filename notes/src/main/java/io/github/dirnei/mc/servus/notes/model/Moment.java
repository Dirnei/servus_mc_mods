package io.github.dirnei.mc.servus.notes.model;

/** The time an edit happens: the in-game day and the real time in milliseconds since the epoch. */
public record Moment(long day, long epochMillis) {
}
