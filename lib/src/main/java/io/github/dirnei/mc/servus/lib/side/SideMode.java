package io.github.dirnei.mc.servus.lib.side;

/** What a block side does for automation. */
public enum SideMode {
    AUTO(true, true),
    INPUT(true, false),
    OUTPUT(false, true),
    BOTH(true, true),
    DISABLED(false, false);

    private static final SideMode[] VALUES = values();

    private final boolean insert;
    private final boolean extract;

    SideMode(boolean insert, boolean extract) {
        this.insert = insert;
        this.extract = extract;
    }

    /** Whether neighbours may insert through this side. */
    public boolean allowsInsert() {
        return insert;
    }

    /** Whether neighbours may extract and the block may push through this side. */
    public boolean allowsExtract() {
        return extract;
    }

    public SideMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public static SideMode byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : AUTO;
    }
}
