package io.github.dirnei.mc.servus.foundation.block;

import io.github.dirnei.mc.servus.foundation.ui.RecipeGridLayout;

/**
 * Menu button ids, synced editor states and ghost grid position of the recipe
 * grid view, shared by every block that edits or orders recipes in it. Common code.
 */
public final class RecipeEditorButtons {

    public static final int OPEN_NEW = 102;
    public static final int SAVE = 103;
    public static final int DELETE = 104;
    public static final int CANCEL = 105;
    /** Switches the recipe's mode, for editors that show the mode button. */
    public static final int MODE = 106;
    /** Empties every ghost cell. */
    public static final int CLEAR = 107;

    /** Editor state: the popup is closed. */
    public static final int EDIT_CLOSED = -1;
    /** Editor state: the popup edits a new recipe. Any value from 0 up is the index of the recipe being edited. */
    public static final int EDIT_NEW = -2;

    private RecipeEditorButtons() {
    }

    /** Left edge of the ghost grid of a popup at {@code popupX}, where a menu puts its first pattern slot. */
    public static int gridX(int popupX) {
        return popupX + RecipeGridLayout.cellX(0);
    }

    /** Top edge of the ghost grid of a popup at {@code popupY}, with or without a title row. */
    public static int gridY(int popupY, boolean titled) {
        return popupY + RecipeGridLayout.cellY(0, titled);
    }
}
