package io.github.dirnei.mc.servus.foundation.logistics.duct;

import java.util.Locale;

/**
 * What one line of a duct side does with the block beside it, as three flags.
 * In: the side takes from the block (items and fluid the block pushes or fills
 * into the duct; fluid and energy the duct drains). Out: the side delivers to
 * the block. Pull: the duct also extracts items from the block, only with In
 * and only on the Item Pipe. Without any flag the side does not connect.
 * Pure Java.
 */
public enum DuctMode {
    NONE(false, false, false),
    IN(true, false, false),
    OUT(false, true, false),
    IN_OUT(true, true, false),
    IN_PULL(true, false, true),
    IN_OUT_PULL(true, true, true);

    private final boolean in;
    private final boolean out;
    private final boolean pull;

    DuctMode(boolean in, boolean out, boolean pull) {
        this.in = in;
        this.out = out;
        this.pull = pull;
    }

    /** The mode with these flags; Pull without In is dropped. */
    public static DuctMode of(boolean in, boolean out, boolean pull) {
        if (!in) {
            return out ? OUT : NONE;
        }
        if (out) {
            return pull ? IN_OUT_PULL : IN_OUT;
        }
        return pull ? IN_PULL : IN;
    }

    public boolean in() {
        return in;
    }

    public boolean out() {
        return out;
    }

    public boolean pull() {
        return pull;
    }

    /** Whether the side connects to its block at all. */
    public boolean connects() {
        return this != NONE;
    }

    /** In switched; switching In off also drops Pull. */
    public DuctMode toggleIn() {
        return of(!in, out, pull);
    }

    public DuctMode toggleOut() {
        return of(in, !out, pull);
    }

    /** Pull switched; switching it on also switches In on. */
    public DuctMode togglePull() {
        return pull ? of(in, out, false) : of(true, out, true);
    }

    /**
     * The mode as the line can hold it: the data line is only connected (In and Out) or not, and only the
     * Item Pipe pulls.
     */
    public DuctMode forLine(Line line) {
        if (line == Line.DATA) {
            return connects() ? IN_OUT : NONE;
        }
        return line == Line.ITEM ? this : of(in, out, false);
    }

    /** The lower case name used for saving and in lang keys. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * The mode of a saved name. Names of the older single mode cycle load as their flags: in and out (both, auto)
     * as In and Out, pull in (input, pull) as In and Pull, push out (output, push) as Out, disabled as none.
     * Unknown names load as In and Out.
     */
    public static DuctMode bySaved(String name) {
        for (DuctMode mode : values()) {
            if (mode.key().equals(name)) {
                return mode;
            }
        }
        return switch (name) {
            case "input", "pull" -> IN_PULL;
            case "output", "push" -> OUT;
            case "disabled" -> NONE;
            default -> IN_OUT;
        };
    }
}
