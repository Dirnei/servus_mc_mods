package io.github.dirnei.mc.servus.shuttles.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class LocationCodeTest {

    @Test
    void northEastUp() {
        assertEquals("N12.E5+2", LocationCode.of(5, 2, -12));
    }

    @Test
    void southWestDown() {
        assertEquals("S7.W3-1", LocationCode.of(-3, -1, 7));
    }

    @Test
    void zeroDistancesAreNorthAndEast() {
        assertEquals("N8.E0+0", LocationCode.of(0, 0, -8));
        assertEquals("N0.W4+0", LocationCode.of(-4, 0, 0));
        assertEquals("N0.E0+0", LocationCode.of(0, 0, 0));
    }

    @Test
    void zeroHeightKeepsItsSign() {
        assertEquals("S2.E1+0", LocationCode.of(1, 0, 2));
    }

    @Test
    void negativeHeight() {
        assertEquals("N1.E1-12", LocationCode.of(1, -12, -1));
    }

    @Test
    void closestOfThreeBaysGivesTheShelfCode() {
        List<LocationCode.Offset> shelf = List.of(new LocationCode.Offset(-4, 0, -8), new LocationCode.Offset(-3, 0, -8),
                new LocationCode.Offset(-2, 0, -8));
        assertEquals(2, LocationCode.codeBay(shelf));
        assertEquals("N8.W2+0", LocationCode.ofShelf(shelf));
    }

    @Test
    void controllerBesideTheMiddleBayPicksTheMiddle() {
        List<LocationCode.Offset> shelf = List.of(new LocationCode.Offset(-1, 0, -8), new LocationCode.Offset(0, 0, -8),
                new LocationCode.Offset(1, 0, -8));
        assertEquals(1, LocationCode.codeBay(shelf));
        assertEquals("N8.E0+0", LocationCode.ofShelf(shelf));
    }

    @Test
    void equalDistancePicksTheLeftmost() {
        List<LocationCode.Offset> shelf = List.of(new LocationCode.Offset(5, 0, -3), new LocationCode.Offset(3, 0, -5));
        assertEquals(0, LocationCode.codeBay(shelf));
    }

    @Test
    void shelfOfOne() {
        assertEquals("S7.W3-1", LocationCode.ofShelf(List.of(new LocationCode.Offset(-3, -1, 7))));
    }

    @Test
    void stackedShelfDiffersByItsHeight() {
        List<LocationCode.Offset> top = List.of(new LocationCode.Offset(-4, 1, -8), new LocationCode.Offset(-3, 1, -8),
                new LocationCode.Offset(-2, 1, -8));
        assertEquals("N8.W2+1", LocationCode.ofShelf(top));
    }

    @Test
    void emptyShelfIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> LocationCode.codeBay(List.of()));
    }
}
