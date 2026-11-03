package io.github.dirnei.mc.servus.notes.model;

import java.util.Locale;

/** Why an edit was not applied. */
public enum Refusal {
    NOT_ALLOWED,
    LIMIT_LISTS,
    LIMIT_TASKS,
    TOO_LONG,
    EMPTY,
    NOT_FOUND,
    /** Switching to Team while in no FTB Teams party. */
    NO_PARTY,
    LIMIT_GUESTS,
    LIMIT_GOALS;

    /** The lower case name, used in payloads and lang keys. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
