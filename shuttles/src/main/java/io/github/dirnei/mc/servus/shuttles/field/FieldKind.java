package io.github.dirnei.mc.servus.shuttles.field;

/** What a field grows: crops on farmland or trees on spots. Pure Java. */
public enum FieldKind {
    CROPS,
    TREES;

    private static final FieldKind[] VALUES = values();

    /** The kind of that name; an unknown or missing name reads as {@link #CROPS}. */
    public static FieldKind byName(String name) {
        for (FieldKind kind : VALUES) {
            if (kind.name().equals(name)) {
                return kind;
            }
        }
        return CROPS;
    }

    /** The kind of that ordinal; out of range reads as {@link #CROPS}. */
    public static FieldKind byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : CROPS;
    }

    public boolean trees() {
        return this == TREES;
    }
}
