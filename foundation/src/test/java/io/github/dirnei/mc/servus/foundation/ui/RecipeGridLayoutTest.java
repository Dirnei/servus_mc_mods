package io.github.dirnei.mc.servus.foundation.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.foundation.ui.RecipeGridLayout.Box;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeGridLayoutTest {

    @Test
    void partsDoNotOverlap() {
        for (boolean titled : new boolean[] {false, true}) {
            List<Box> parts = RecipeGridLayout.parts(titled);
            for (int i = 0; i < parts.size(); i++) {
                for (int j = i + 1; j < parts.size(); j++) {
                    assertFalse(parts.get(i).overlaps(parts.get(j)), parts.get(i) + " overlaps " + parts.get(j) + ", titled " + titled);
                }
            }
        }
    }

    @Test
    void partsInsidePopup() {
        for (boolean titled : new boolean[] {false, true}) {
            for (Box part : RecipeGridLayout.parts(titled)) {
                assertTrue(part.x() >= 2 && part.y() >= 2 && part.right() <= RecipeGridLayout.WIDTH - 2
                        && part.bottom() <= RecipeGridLayout.HEIGHT - 2, part + " leaves the popup's frame, titled " + titled);
            }
            for (Box action : RecipeGridLayout.actions(RecipeGridLayout.MAX_ACTIONS)) {
                assertTrue(action.x() >= RecipeGridLayout.ACTIONS.x() && action.right() <= RecipeGridLayout.ACTIONS.right(),
                        action + " leaves the action row");
            }
        }
    }

    @Test
    void actionsSpreadLeftMiddleRight() {
        Box row = RecipeGridLayout.ACTIONS;
        assertEquals(row.x(), RecipeGridLayout.actions(1).get(0).x());
        for (int count = 2; count <= RecipeGridLayout.MAX_ACTIONS; count++) {
            List<Box> actions = RecipeGridLayout.actions(count);
            assertEquals(count, actions.size());
            assertEquals(row.x(), actions.get(0).x(), "first on the left");
            assertEquals(row.right(), actions.get(count - 1).right(), "last on the right");
            for (int i = 1; i < count; i++) {
                assertTrue(actions.get(i - 1).right() < actions.get(i).x(), "actions in order and apart, " + actions);
            }
            if (count > 2) {
                int middleLeft = actions.get(1).x() - row.x();
                int middleRight = row.right() - actions.get(count - 2).right();
                assertTrue(Math.abs(middleLeft - middleRight) <= 1, "the middle actions are centred, " + actions);
            }
        }
        assertThrows(IllegalArgumentException.class, () -> RecipeGridLayout.actions(RecipeGridLayout.MAX_ACTIONS + 1));
    }

    @Test
    void cellsMatchMenuSlots() {
        int popupX = -23;
        int popupY = 40;
        for (boolean titled : new boolean[] {false, true}) {
            for (int cell = 0; cell < 9; cell++) {
                int slotX = RecipeEditorButtons.gridX(popupX) + (cell % 3) * RecipeGridLayout.CELL;
                int slotY = RecipeEditorButtons.gridY(popupY, titled) + (cell / 3) * RecipeGridLayout.CELL;
                assertEquals(popupX + RecipeGridLayout.cellX(cell), slotX);
                assertEquals(popupY + RecipeGridLayout.cellY(cell, titled), slotY);
                assertEquals(cell, RecipeGridLayout.cellAt(slotX - popupX + 8, slotY - popupY + 8, titled), "the cell under its own slot");
            }
            assertEquals(-1, RecipeGridLayout.cellAt(RecipeGridLayout.result(titled).x(), RecipeGridLayout.result(titled).y(), titled));
        }
    }
}
