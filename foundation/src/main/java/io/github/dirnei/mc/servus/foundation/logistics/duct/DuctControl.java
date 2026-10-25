package io.github.dirnei.mc.servus.foundation.logistics.duct;

import java.util.Locale;

/**
 * When one line of a duct side moves anything: always, only while the duct
 * receives a redstone signal, only while it does not, or never. A side that
 * is not active keeps its connection and only stops moving. Pure Java.
 */
public enum DuctControl {
    ALWAYS,
    POWERED,
    UNPOWERED,
    OFF;

    private static final DuctControl[] VALUES = values();

    /** Whether the side moves anything while the duct is {@code powered} or not. */
    public boolean active(boolean powered) {
        return switch (this) {
            case ALWAYS -> true;
            case POWERED -> powered;
            case UNPOWERED -> !powered;
            case OFF -> false;
        };
    }

    /** The next Control of the button cycle: Always, While powered, While unpowered, Off, Always. */
    public DuctControl next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    /** The stored id. */
    public int id() {
        return ordinal();
    }

    /** The Control of a stored id; unknown ids read as {@link #ALWAYS}. */
    public static DuctControl byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : ALWAYS;
    }

    /** The lower case name used in lang keys. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
