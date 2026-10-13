package io.github.dirnei.mc.servus.foundation.block;

/**
 * Menu button ids, synced editor states and ghost grid position of the recipe
 * editor popup, shared by every block that edits recipes in it. Common code.
 */
public final class RecipeEditorButtons {

    public static final int OPEN_NEW = 102;
    public static final int SAVE = 103;
    public static final int DELETE = 104;
    public static final int CANCEL = 105;
    /** Opens the editor on recipe {@code i} with the id {@code OPEN_EDIT_FIRST + i}. */
    public static final int OPEN_EDIT_FIRST = 200;

    /** Editor state: the popup is closed. */
    public static final int EDIT_CLOSED = -1;
    /** Editor state: the popup edits a new recipe. Any value from 0 up is the index of the recipe being edited. */
    public static final int EDIT_NEW = -2;

    /** Offset of the ghost grid from the popup's top left corner. */
    public static final int GRID_OFFSET_X = 6;
    public static final int GRID_OFFSET_Y = 16;

    private RecipeEditorButtons() {
    }

    /** Left edge of the ghost grid of a popup at {@code popupX}, where a menu puts its first pattern slot. */
    public static int gridX(int popupX) {
        return popupX + GRID_OFFSET_X;
    }

    /** Top edge of the ghost grid of a popup at {@code popupY}. */
    public static int gridY(int popupY) {
        return popupY + GRID_OFFSET_Y;
    }
}
