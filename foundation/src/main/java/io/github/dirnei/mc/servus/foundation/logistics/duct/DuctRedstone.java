package io.github.dirnei.mc.servus.foundation.logistics.duct;

import java.util.Locale;

/**
 * The redstone setting of one line of a duct side: ignore the signal, run
 * only while the duct receives a redstone signal, or only while it does not.
 * A side runs when it is switched on and its redstone setting is met. Pure Java.
 */
public enum DuctRedstone {
    IGNORE,
    POWERED,
    UNPOWERED;

    private static final DuctRedstone[] VALUES = values();
    /** The stored id the older Control setting used for Off. */
    private static final int LEGACY_OFF_ID = 3;

    /** Whether the setting lets the side run while the duct is {@code powered} or not. */
    public boolean met(boolean powered) {
        return switch (this) {
            case IGNORE -> true;
            case POWERED -> powered;
            case UNPOWERED -> !powered;
        };
    }

    /** Whether a side with this setting moves anything: it is {@code on} and the setting is met. */
    public boolean active(boolean on, boolean powered) {
        return on && met(powered);
    }

    /** The next setting of the button cycle: ignore, while powered, while unpowered, ignore. */
    public DuctRedstone next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    /** The stored id. */
    public int id() {
        return ordinal();
    }

    /** The setting of a stored id; unknown ids, and the older Control's Off, read as {@link #IGNORE}. */
    public static DuctRedstone byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : IGNORE;
    }

    /** Whether a stored id of the older Control setting meant Off, which loads as switched off. */
    public static boolean legacyOff(int id) {
        return id == LEGACY_OFF_ID;
    }

    /** The lower case name used in lang keys. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
