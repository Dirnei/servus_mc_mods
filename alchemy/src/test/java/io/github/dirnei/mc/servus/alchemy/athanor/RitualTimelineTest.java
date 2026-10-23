package io.github.dirnei.mc.servus.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Phases;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Sounds;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CrystalCluster;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RitualTimeline;
import java.util.List;
import org.junit.jupiter.api.Test;

class RitualTimelineTest {

    private static final RitualTimeline TIMELINE = new RitualTimeline(new AthanorRitual("citrinitas", "minecraft:amethyst_block", 1,
            0xB070FF, 0xFFB000, 160, new Phases(36, 60, 80, 150), Sounds.NONE, List.of()));

    @Test
    void offeringHoversUntilItBursts() {
        assertTrue(TIMELINE.offeringShown(0));
        assertTrue(TIMELINE.offeringShown(36));
        assertFalse(TIMELINE.offeringShown(38));
        assertEquals(0, TIMELINE.offeringRise(0));
        assertEquals(1, TIMELINE.offeringRise(36));
    }

    @Test
    void flameTurnsToTheResultAfterTheFlash() {
        assertEquals(0xB070FF, TIMELINE.flameColor(100));
        assertEquals(0xFFB000, TIMELINE.flameColor(160));
        assertEquals(1, TIMELINE.flameTint(30));
    }

    @Test
    void streamsLeaveAfterTheBurstOneAfterAnother() {
        assertTrue(TIMELINE.stream(36, 0, 0) < 0, "nothing flies before the burst");
        assertTrue(TIMELINE.stream(45, 0, 0) > 0);
        assertTrue(TIMELINE.stream(45, 2, 0) < TIMELINE.stream(45, 0, 0), "the third stream leaves later");
    }

    @Test
    void everyBlockIsFullAndGrownByTheFlash() {
        for (int index = 0; index < 3; index++) {
            assertEquals(0, TIMELINE.growth(80, index, 3));
            assertEquals(1, TIMELINE.fill(150, index));
            assertEquals(1, TIMELINE.growth(150, index, 3));
        }
        assertTrue(TIMELINE.flash(149) < 0);
        assertEquals(1, TIMELINE.flash(160));
    }

    private static final RitualTimeline RUBEDO = new RitualTimeline(new AthanorRitual("rubedo", "minecraft:ghast_tear", 2,
            0xFF2A2A, 0xE04020, 160, new Phases(36, 60, 80, 144), Sounds.NONE, List.of()));

    @Test
    void obelisksCrackThenRiseBeforeTheFlash() {
        for (int index = 0; index < 4; index++) {
            assertEquals(0, RUBEDO.cracks(60, index), "the cracks wait for the drop");
            assertEquals(1, RUBEDO.cracks(80 + RitualTimeline.delay(index), index), "cracked before it rises");
            assertEquals(0, RUBEDO.rise(80, index));
            assertEquals(1, RUBEDO.rise(143, index), "risen before the flash");
        }
    }

    @Test
    void grownCrystalTurnsItsColourBeforeTheFlash() {
        assertEquals(0, RUBEDO.recolor(60));
        assertTrue(RUBEDO.recolor(100) > 0 && RUBEDO.recolor(100) < 1);
        assertEquals(1, RUBEDO.recolor(143));
        assertEquals(0, RUBEDO.answer(0), "the crystal answers only once the offering rises");
        assertTrue(RUBEDO.answer(14) > 0.5F);
        assertEquals(0, RUBEDO.answer(60));
    }

    @Test
    void clusterIsFullSizeOnceGrown() {
        for (CrystalCluster.Spike spike : CrystalCluster.SPIKES) {
            assertEquals(0, CrystalCluster.scale(spike, 0));
            assertEquals(1, CrystalCluster.scale(spike, 1), 1e-6);
        }
        CrystalCluster.Spike tallest = CrystalCluster.SPIKES.get(0);
        assertEquals(0, CrystalCluster.scale(tallest, 0.4F), "the big spike grows last");
        assertEquals(3.0, CrystalCluster.ROOT + CrystalCluster.top(tallest), 0.05,
                "from the vat's floor the tallest spike reaches the top of the tanks, three blocks up");
    }
}
