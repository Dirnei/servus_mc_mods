package io.github.dirnei.mc.servus.notes.model;

/** What one player may do with one list; see {@link Access} for what each right covers. */
public record Rights(boolean see, boolean edit, boolean tick, boolean manage) {

    public static final Rights ALL = new Rights(true, true, true, true);
    public static final Rights NONE = new Rights(false, false, false, false);
}
