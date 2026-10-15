package io.github.dirnei.mc.servus.alchemy.gnome;

import java.util.Locale;

/** The three gnomes of the elementals sheet; each has its own look, prop and work. */
public enum GnomeKind {
    /** The worker of a Gnome Hollow, with a pickaxe. */
    MINER,
    /** The old gnome's young apprentice, with a lit lantern; he leads to the Ancient Hollow. */
    LANTERN,
    /** Paracelsus' old gnome at the Ancient Hollow, with a patched hat and a Salt Crystal. */
    HOUSE;

    private static final GnomeKind[] VALUES = values();

    /** The lower case name used in saved data and resource paths. */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The kind of a {@link #serializedName()}, the Old Miner for an unknown name. */
    public static GnomeKind byName(String name) {
        for (GnomeKind kind : VALUES) {
            if (kind.serializedName().equals(name)) {
                return kind;
            }
        }
        return MINER;
    }

    /** The kind at {@link #ordinal()}, the Old Miner for an unknown index. */
    public static GnomeKind byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : MINER;
    }
}
