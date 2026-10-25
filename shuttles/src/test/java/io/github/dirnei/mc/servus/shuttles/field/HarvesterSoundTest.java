package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.field.HarvesterSound.Cue;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Part;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HarvesterSoundTest {

    private static Optional<Cue> cue(Part part, double progress) {
        return HarvesterSound.cue(part, HarvesterTimeline.of(part, progress));
    }

    private static boolean idles(Part part, double progress) {
        return HarvesterSound.idles(part, HarvesterTimeline.of(part, progress));
    }

    @Test
    void idlesFromTheRiseUntilItFolds() {
        assertFalse(idles(Part.UNFOLD, 0.1), "doors opening");
        assertTrue(idles(Part.UNFOLD, 0.8), "crane rising");
        assertTrue(idles(Part.READY, 0));
        assertTrue(idles(Part.SAW, 0.1), "reaching");
        assertFalse(idles(Part.SAW, 0.7), "cutting");
        assertTrue(idles(Part.TOPPLE, 0.5), "waiting while the tree falls");
        assertTrue(idles(Part.TOPPLE, 0.95), "swinging home");
        assertFalse(idles(Part.FOLD, 0.1));
    }

    @Test
    void unfoldingOpensLandsAndRises() {
        assertEquals(Optional.of(Cue.DOORS_OPEN), cue(Part.UNFOLD, 0.05));
        assertEquals(Optional.empty(), cue(Part.UNFOLD, 0.4), "walls still on their way down");
        assertEquals(Optional.of(Cue.WALLS_DOWN), cue(Part.UNFOLD, 0.58));
        assertEquals(Optional.of(Cue.CRANE_UP), cue(Part.UNFOLD, 0.7));
        assertEquals(Optional.empty(), cue(Part.READY, 0));
    }

    @Test
    void cutsAndStopsWithTheTree() {
        assertEquals(Optional.empty(), cue(Part.SAW, 0.3));
        assertEquals(Optional.of(Cue.CUT), cue(Part.SAW, 0.7));
        assertEquals(Optional.of(Cue.STOP), cue(Part.TOPPLE, 0.02));
        assertEquals(Optional.empty(), cue(Part.TOPPLE, 0.5));
    }

    @Test
    void foldingSinksRaisesAndShuts() {
        assertEquals(Optional.of(Cue.CRANE_DOWN), cue(Part.FOLD, 0.1));
        assertEquals(Optional.of(Cue.WALLS_UP), cue(Part.FOLD, 0.5));
        assertEquals(Optional.empty(), cue(Part.FOLD, 0.75), "doors still closing");
        assertEquals(Optional.of(Cue.DOORS_SHUT), cue(Part.FOLD, 0.98));
    }

    @Test
    void onlyTheChainsawCuesArePerTree() {
        for (Cue cue : Cue.values()) {
            assertEquals(cue == Cue.CUT || cue == Cue.STOP, cue.chainsaw(), cue.name());
        }
    }
}
