package io.github.dirnei.mc.servus.shuttles.field;

/** Whether a Field Head serves a field, or why not. */
public enum FieldStatus {
    SERVING, NO_FIELD, TOO_LARGE, OTHER_HEAD;

    private static final FieldStatus[] VALUES = values();

    public static FieldStatus byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NO_FIELD;
    }
}
