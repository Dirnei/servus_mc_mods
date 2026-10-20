package io.github.dirnei.mc.servus.brewery.rausch;

import java.util.Locale;

/** The Rausch levels, from the mildest to the strongest. Pure Java. */
public enum RauschLevel {
    TIPSY,
    MERRY,
    DRUNK,
    PLASTERED;

    /** The level's name in data and registry ids. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
