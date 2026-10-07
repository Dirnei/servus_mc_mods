package io.github.dirnei.mc.servus.lib.side;

/**
 * What a block side does for automation. Input, Output and Both are passive:
 * neighbours move the items. Pull and Push are active: the block moves them.
 * Auto is the block's unconfigured behaviour, Disabled (shown as Off) allows nothing.
 */
public enum SideMode {
    AUTO(true, true, false, true),
    INPUT(true, false, false, false),
    OUTPUT(false, true, false, false),
    BOTH(true, true, false, false),
    DISABLED(false, false, false, false),
    PULL(true, false, true, false),
    PUSH(false, true, false, true);

    private static final SideMode[] VALUES = values();

    private final boolean insert;
    private final boolean extract;
    private final boolean pull;
    private final boolean push;

    SideMode(boolean insert, boolean extract, boolean pull, boolean push) {
        this.insert = insert;
        this.extract = extract;
        this.pull = pull;
        this.push = push;
    }

    /** Whether neighbours may insert through this side. */
    public boolean allowsInsert() {
        return insert;
    }

    /** Whether neighbours may extract through this side. */
    public boolean allowsExtract() {
        return extract;
    }

    /** Whether the block itself takes items from the neighbour on this side. */
    public boolean pulls() {
        return pull;
    }

    /** Whether the block itself gives items to the neighbour on this side. */
    public boolean pushes() {
        return push;
    }

    /** The lower case key used for lang entries and glyphs. */
    public String key() {
        return this == DISABLED ? "off" : name().toLowerCase(java.util.Locale.ROOT);
    }

    public static SideMode byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : AUTO;
    }
}
