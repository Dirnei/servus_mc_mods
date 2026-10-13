package io.github.dirnei.mc.servus.foundation.machine.anchor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.machine.anchor.AnchorArea.Shift;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AnchorAreaTest {

    @Test
    void sizesAreOneNineTwentyFive() {
        assertEquals(1, AnchorArea.of(0, 0, 0).size());
        assertEquals(9, AnchorArea.of(1, 0, 0).size());
        assertEquals(25, AnchorArea.of(2, 0, 0).size());
        assertEquals(3, AnchorArea.of(1, 0, 0).side());
    }

    @Test
    void offsetClampedToRadius() {
        AnchorArea area = AnchorArea.of(1, 5, -7);
        assertEquals(1, area.offsetX());
        assertEquals(-1, area.offsetZ());
        AnchorArea none = AnchorArea.of(0, 2, 2);
        assertEquals(0, none.offsetX());
        assertEquals(0, none.offsetZ());
    }

    @Test
    void ownChunkAlwaysInside() {
        for (int radius = 0; radius <= 2; radius++) {
            for (int ox = -radius; ox <= radius; ox++) {
                for (int oz = -radius; oz <= radius; oz++) {
                    AnchorArea area = AnchorArea.of(radius, ox, oz);
                    assertTrue(area.contains(10, -4, 10, -4), "radius " + radius + " offset " + ox + "," + oz);
                    assertTrue(area.chunks(10, -4).contains(AnchorArea.pack(10, -4)));
                }
            }
        }
    }

    @Test
    void shiftStopsAtTheLimit() {
        AnchorArea area = AnchorArea.of(1, 0, 0);
        AnchorArea north = area.shift(Shift.NORTH);
        assertEquals(-1, north.offsetZ());
        assertFalse(north.canShift(Shift.NORTH));
        assertSame(north, north.shift(Shift.NORTH));
        assertTrue(north.canShift(Shift.SOUTH));
        AnchorArea east = area.shift(Shift.EAST);
        assertEquals(1, east.offsetX());
        assertFalse(east.canShift(Shift.EAST));
        assertFalse(AnchorArea.of(0, 0, 0).canShift(Shift.WEST));
        assertEquals(Shift.WEST, Shift.byId(3));
        assertEquals(null, Shift.byId(4));
    }

    @Test
    void shrinkingRadiusClampsOffset() {
        AnchorArea area = AnchorArea.of(1, 2, -2);
        assertEquals(1, area.offsetX());
        assertEquals(-1, area.offsetZ());
        assertEquals(9, area.size());
        AnchorArea gone = AnchorArea.of(0, area.offsetX(), area.offsetZ());
        assertEquals(0, gone.offsetX());
        assertEquals(0, gone.offsetZ());
    }

    @Test
    void chunksListTheSquare() {
        AnchorArea north = AnchorArea.of(1, 0, -1);
        List<Long> chunks = north.chunks(0, 0);
        assertEquals(9, chunks.size());
        Set<Long> expected = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -2; z <= 0; z++) {
                expected.add(AnchorArea.pack(x, z));
            }
        }
        assertEquals(expected, new HashSet<>(chunks));
        long packed = AnchorArea.pack(-3, 7);
        assertEquals(-3, AnchorArea.unpackX(packed));
        assertEquals(7, AnchorArea.unpackZ(packed));
    }
}
