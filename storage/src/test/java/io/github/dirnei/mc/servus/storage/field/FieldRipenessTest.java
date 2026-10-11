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

    @Test
    void openPlots() {
        FieldRipeness<String> field = new FieldRipeness<>();
        field.update("a", PlotState.TILLABLE);
        field.update("b", PlotState.PLANTABLE);
        field.update("c", PlotState.RIPE);
        assertEquals(1, field.tillable());
        assertEquals(1, field.plantable());
        assertEquals(1, field.planted());
        assertEquals(PlotState.TILLABLE, field.state("a"));
        field.update("a", PlotState.PLANTABLE);
        assertEquals(0, field.tillable());
        assertEquals(2, field.plantable());
        field.update("b", PlotState.GROWING);
        assertEquals(1, field.plantable());
        assertEquals(2, field.planted());
        field.update("c", PlotState.BLOCKED);
        assertEquals(PlotState.BLOCKED, field.state("c"));
        assertEquals(1, field.planted());
        assertEquals(PlotState.BLOCKED, field.state("unknown"));
        field.clear();
        assertEquals(0, field.plantable());
    }
}
