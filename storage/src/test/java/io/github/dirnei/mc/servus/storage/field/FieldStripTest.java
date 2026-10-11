package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FieldStripTest {

    private static final List<FieldPos> RUN_X = List.of(new FieldPos(0, 64, 0), new FieldPos(1, 64, 0), new FieldPos(2, 64, 0));

    @Test
    void plotsExcludeRailsAndReachFour() {
        Set<FieldPos> plots = FieldStrip.plots(RUN_X, 4);
        assertEquals(11 * 9 - 3, plots.size());
        assertFalse(plots.contains(new FieldPos(1, 64, 0)));
        assertTrue(plots.contains(new FieldPos(1, 64, 4)));
        assertFalse(plots.contains(new FieldPos(1, 64, 5)));
        assertTrue(plots.contains(new FieldPos(-4, 64, -4)));
        assertFalse(plots.contains(new FieldPos(-5, 64, 0)));
        assertFalse(plots.contains(new FieldPos(1, 63, 1)));
    }

    @Test
    void straightRunAlongXIsOneBox() {
        List<FieldStrip.Box> boxes = FieldStrip.boxes(RUN_X, 4);
        assertEquals(List.of(new FieldStrip.Box(-4, 63, -4, 7, 64, 5)), boxes);
        FieldStrip.Box box = boxes.get(0);
        assertTrue(box.contains(1, 63, 4));
        assertFalse(box.contains(1, 63, 5));
        assertFalse(box.contains(1, 64, 1));
        assertTrue(box.contains(-4, 63, 0));
        assertFalse(box.contains(7, 63, 0));
    }

    @Test
    void runAlongZIsOneBoxPerRail() {
        List<FieldPos> run = List.of(new FieldPos(0, 64, 0), new FieldPos(0, 64, 1));
        assertEquals(2, FieldStrip.boxes(run, 4).size());
    }

    @Test
    void gapInRowSplitsBoxes() {
        List<FieldPos> rails = List.of(new FieldPos(0, 64, 0), new FieldPos(1, 64, 0), new FieldPos(5, 64, 0));
        assertEquals(List.of(new FieldStrip.Box(-4, 63, -4, 6, 64, 5), new FieldStrip.Box(1, 63, -4, 10, 64, 5)),
                FieldStrip.boxes(rails, 4));
    }

    @Test
    void negativeRows() {
        List<FieldStrip.Box> boxes = FieldStrip.boxes(List.of(new FieldPos(-3, -10, -7)), 4);
        assertEquals(List.of(new FieldStrip.Box(-7, -11, -11, 2, -10, -2)), boxes);
    }

    @Test
    void radiusTwoReachesTwo() {
        Set<FieldPos> plots = FieldStrip.plots(RUN_X, 2);
        assertEquals(7 * 5 - 3, plots.size());
        assertTrue(plots.contains(new FieldPos(1, 64, 2)));
        assertFalse(plots.contains(new FieldPos(1, 64, 3)));
        assertEquals(List.of(new FieldStrip.Box(-2, 63, -2, 5, 64, 3)), FieldStrip.boxes(RUN_X, 2));
    }
}
