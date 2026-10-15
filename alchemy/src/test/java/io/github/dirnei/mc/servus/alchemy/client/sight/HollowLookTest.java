package io.github.dirnei.mc.servus.alchemy.client.sight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.client.sight.HollowLook.Cell;
import io.github.dirnei.mc.servus.alchemy.client.sight.HollowLook.Crack;
import java.util.List;
import org.junit.jupiter.api.Test;

class HollowLookTest {

    private static final int NONE = 0;

    /** A crack along row 0 from x 0 to 4, positions 0 to 1, with a nodule at its end. */
    private static int mask(int x, int y) {
        if (y != 0 || x > 4) {
            return NONE;
        }
        int red = Math.round(x / 4F * 255);
        int blue = x == 4 ? 255 : 0;
        return 0xFF000000 | red << 16 | blue;
    }

    /** A 5 by 5 mouth from (5, 5) to (9, 9), its middle at (7, 7). */
    private static int cave(int x, int y) {
        return x >= 5 && x <= 9 && y >= 5 && y <= 9 ? 0xFF101010 : NONE;
    }

    private static int crystals(int x, int y) {
        return (x == 4 && y == 4) || (x == 10 && y == 4) ? 0xFFFFFFFF : NONE;
    }

    private static final HollowLook LOOK = HollowLook.read(HollowLookTest::mask, HollowLookTest::cave, HollowLookTest::crystals);

    @Test
    void emptyRipenessFillsOnlyTheStartOfEachCrack() {
        List<Crack> filled = LOOK.filled(0);
        assertEquals(1, filled.size());
        assertEquals(0, filled.get(0).x());
    }

    @Test
    void halfRipeFillsTheFirstHalf() {
        assertEquals(List.of(0, 1, 2), LOOK.filled(0.55F).stream().map(Crack::x).toList());
    }

    @Test
    void ripeFillsEveryCrack() {
        assertEquals(5, LOOK.filled(1).size());
    }

    @Test
    void nodulesBreakOutOnlyNearRipe() {
        assertTrue(LOOK.nodules(0.8F).isEmpty());
        assertTrue(LOOK.nodules(0.9F).isEmpty(), "the nodule at the crack's end is not filled yet");
        List<Crack> ripe = LOOK.nodules(1);
        assertEquals(1, ripe.size());
        assertEquals(4, ripe.get(0).x());
    }

    @Test
    void crystalsNeedABoundHollowAndThirtyPercent() {
        assertTrue(LOOK.crystals(1, false).isEmpty());
        assertTrue(LOOK.crystals(0.2F, true).isEmpty());
        assertEquals(2, LOOK.crystals(0.3F, true).size());
    }

    @Test
    void caveRingsCountOutFromTheMiddle() {
        assertEquals(List.of(new Cell(7, 7, 0)), LOOK.caveRing(0));
        assertEquals(8, LOOK.caveRing(1).size());
        assertEquals(16, LOOK.caveRing(2).size());
    }

    @Test
    void caveOpensRingByRing() {
        assertTrue(LOOK.cave(0).isEmpty());
        assertEquals(1, LOOK.cave(0.34F).size());
        assertEquals(9, LOOK.cave(0.67F).size());
        assertEquals(25, LOOK.cave(1).size());
        assertEquals(3, LOOK.ringsOpen(1));
    }

    @Test
    void flowRunsFromTheStartToTheEnd() {
        float phase = (float) Math.PI / 2;
        assertEquals(1, HollowLook.flow(phase, 0), 1e-5);
        assertTrue(HollowLook.flow(phase, 0.5F) < 1);
        float later = phase + 4.5F;
        assertEquals(1, HollowLook.flow(later, 0.5F), 1e-5);
    }

    @Test
    void flowStaysBetweenDimAndFull() {
        for (int i = 0; i < 100; i++) {
            float brightness = HollowLook.flow(i * 0.37F, (i % 10) / 10F);
            assertTrue(brightness >= 0.2F - 1e-5 && brightness <= 1 + 1e-5, "brightness " + brightness);
        }
    }
}
