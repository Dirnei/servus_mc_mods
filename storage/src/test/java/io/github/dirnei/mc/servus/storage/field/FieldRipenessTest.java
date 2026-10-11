package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldRipenessTest {

    @Test
    void countsAndShare() {
        FieldRipeness<String> field = new FieldRipeness<>();
        field.update("a", true, false);
        field.update("b", true, true);
        field.update("c", true, false);
        field.update("d", true, false);
        assertEquals(4, field.planted());
        assertEquals(1, field.ripe());
        assertEquals(0.25, field.share());
        assertTrue(field.isRipe("b"));
    }

    @Test
    void plotChangesState() {
        FieldRipeness<String> field = new FieldRipeness<>();
        field.update("a", true, true);
        field.update("a", true, false);
        assertEquals(1, field.planted());
        assertEquals(0, field.ripe());
        field.update("a", false, false);
        assertEquals(0, field.planted());
        field.update("b", false, true);
        assertEquals(1, field.planted());
        field.clear();
        assertEquals(0, field.planted());
        assertEquals(0, field.ripe());
    }

    @Test
    void dueAtThreshold() {
        FieldRipeness<String> field = new FieldRipeness<>();
        for (String plot : new String[] {"a", "b", "c", "d"}) {
            field.update(plot, true, false);
        }
        field.update("a", true, true);
        assertFalse(field.due(0.5));
        field.update("b", true, true);
        assertTrue(field.due(0.5));
    }

    @Test
    void neverDueWhenNothingPlanted() {
        FieldRipeness<String> field = new FieldRipeness<>();
        assertEquals(0, field.share());
        assertFalse(field.due(0));
    }
}
