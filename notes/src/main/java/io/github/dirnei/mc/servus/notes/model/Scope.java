package io.github.dirnei.mc.servus.notes.model;

import java.util.Locale;

/** Who a list is for besides its owner and guests. */
public enum Scope {
    /** Only the owner and the guests. */
    PRIVATE,
    /** The members of the owner's FTB Teams party, kept on the list when it switched to Team. */
    TEAM,
    /** Every player on the server reads it; ops edit it. */
    SERVER;

    /** The lower case name, used in saves and lang keys. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
