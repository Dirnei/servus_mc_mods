package io.github.dirnei.mc.servus.notes.model;

import java.util.Locale;
import java.util.Objects;

/** A player the owner added to a list, with what they may do there. */
public record Guest(Person who, Right right) {

    /** What a guest may do: edit like the owner (without sharing), or only read, tick and claim. */
    public enum Right {
        EDIT,
        READ;

        /** The lower case name, used in saves and lang keys. */
        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public Guest {
        Objects.requireNonNull(who, "who");
        Objects.requireNonNull(right, "right");
    }

    public Guest withRight(Right newRight) {
        return new Guest(who, newRight);
    }
}
