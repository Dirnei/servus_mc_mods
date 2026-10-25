package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TreeSpotsTest {

    @Test
    void evenGrid() {
        assertTrue(TreeSpots.isSpot(0, 0));
        assertTrue(TreeSpots.isSpot(4, 2));
        assertFalse(TreeSpots.isSpot(1, 0));
        assertFalse(TreeSpots.isSpot(2, 3));
        assertFalse(TreeSpots.isSpot(3, 5));
    }

    @Test
    void negativeCoordinates() {
        assertTrue(TreeSpots.isSpot(-2, -4));
        assertFalse(TreeSpots.isSpot(-1, -4));
        assertEquals(Optional.of(new FieldPos(-3, 1, -3)), TreeSpots.square(new FieldPos(-2, 1, -3)));
        assertEquals(Optional.empty(), TreeSpots.square(new FieldPos(-1, 1, 0)));
    }

    @Test
    void squareMembersAndGap() {
        FieldPos corner = new FieldPos(3, 1, 6);
        for (FieldPos member : TreeSpots.members(corner)) {
            assertEquals(Optional.of(corner), TreeSpots.square(member));
        }
        assertEquals(Optional.empty(), TreeSpots.square(new FieldPos(5, 1, 6)), "the third column is the gap");
        assertEquals(Optional.empty(), TreeSpots.square(new FieldPos(3, 1, 8)), "the third row is the gap");
        assertEquals(List.of(corner, new FieldPos(4, 1, 6), new FieldPos(3, 1, 7), new FieldPos(4, 1, 7)), TreeSpots.members(corner));
    }

    @Test
    void squareCutOffByAMissingPlot() {
        FieldPos corner = new FieldPos(0, 1, 0);
        Set<FieldPos> field = Set.of(corner, new FieldPos(1, 1, 0), new FieldPos(0, 1, 1));
        assertFalse(TreeSpots.complete(corner, field::contains));
        assertFalse(TreeSpots.planted(corner, true, field::contains));
        Set<FieldPos> whole = Set.of(corner, new FieldPos(1, 1, 0), new FieldPos(0, 1, 1), new FieldPos(1, 1, 1));
        assertTrue(TreeSpots.complete(corner, whole::contains));
        assertTrue(TreeSpots.planted(new FieldPos(1, 1, 1), true, whole::contains));
    }

    @Test
    void baseOfATree() {
        FieldPos plot = new FieldPos(4, 1, 1);
        assertEquals(List.of(plot), TreeSpots.base(plot, false));
        assertEquals(TreeSpots.members(new FieldPos(3, 1, 0)), TreeSpots.base(plot, true));
        assertEquals(List.of(new FieldPos(5, 1, 1)), TreeSpots.base(new FieldPos(5, 1, 1), true), "a gap plot stands alone");
    }

    @Test
    void kindReadsCropsWhenUnknown() {
        assertEquals(FieldKind.TREES, FieldKind.byName("TREES"));
        assertEquals(FieldKind.CROPS, FieldKind.byName(""));
        assertEquals(FieldKind.CROPS, FieldKind.byId(9));
    }
}
