package io.github.dirnei.mc.servus.lib.block;

/**
 * The menu button ids of the recipe editor popup, shared by every block that
 * configures recipes in it, and the editor states its menus sync.
 */
public final class RecipeEditorButtons {

    public static final int OPEN_NEW = 102;
    public static final int SAVE = 103;
    public static final int DELETE = 104;
    public static final int CANCEL = 105;
    /** Opens the editor on recipe {@code i} with the id {@code OPEN_EDIT_FIRST + i}. */
    public static final int OPEN_EDIT_FIRST = 200;
    /** Room for this many recipe ids after {@link #OPEN_EDIT_FIRST}. */
    public static final int OPEN_EDIT_COUNT = 64;

    /** Editor state: the popup is closed. */
    public static final int EDIT_CLOSED = -1;
    /** Editor state: the popup edits a new recipe. Any value from 0 up is the index of the recipe being edited. */
    public static final int EDIT_NEW = -2;

    private RecipeEditorButtons() {
    }

    /** The recipe index an open edit button stands for, or -1. */
    public static int editIndexOf(int buttonId) {
        int offset = buttonId - OPEN_EDIT_FIRST;
        return offset >= 0 && offset < OPEN_EDIT_COUNT ? offset : -1;
    }
}
