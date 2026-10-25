package io.github.dirnei.mc.servus.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CitrineTower;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CitrineTower.Bud;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CitrineTower.Face;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CitrineTower.Openness;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CitrineTower.Shape;
import java.util.List;
import org.junit.jupiter.api.Test;

class CitrineTowerTest {

    private static Shape full(int seed) {
        return CitrineTower.grown(seed, 1, Openness.ALL);
    }

    @Test
    void nothingGrowsAtTheStart() {
        Shape shape = CitrineTower.grown(0, 0, Openness.ALL);
        assertTrue(shape.tower().isEmpty());
        assertTrue(shape.buds().isEmpty());
    }

    @Test
    void segmentsRiseInOrderBeforeAnyBud() {
        assertEquals(1, CitrineTower.grown(0, CitrineTower.TOWER_SHARE / 6, Openness.ALL).tower().size(), "first segment only");
        assertEquals(2, CitrineTower.grown(0, CitrineTower.TOWER_SHARE / 2, Openness.ALL).tower().size(), "first two segments");
        Shape almost = CitrineTower.grown(0, CitrineTower.TOWER_SHARE - 0.01F, Openness.ALL);
        assertEquals(3, almost.tower().size(), "no tip while the top segment still rises");
        assertTrue(almost.buds().isEmpty(), "buds wait for the tower");
    }

    @Test
    void fullTowerHasItsTipAndEveryBudAtFullStage() {
        for (int seed = 0; seed < CitrineTower.SEEDS; seed++) {
            Shape shape = full(seed);
            assertEquals(4, shape.tower().size(), "three segments and the tip");
            assertEquals(11, shape.buds().size());
            shape.buds().forEach(bud -> assertEquals(4, bud.stage()));
        }
        List<CitrineTower.Box> tower = full(0).tower();
        assertEquals(CitrineTower.FOOT + 18 + 16 + 12 + 2, tower.getLast().y() + tower.getLast().height(), 1e-6);
    }

    @Test
    void sideBudsLeanUpWithinRangeAndTopBudsStandStraight() {
        for (int seed = 0; seed < CitrineTower.SEEDS; seed++) {
            for (Bud bud : full(seed).buds()) {
                if (bud.face() == Face.UP) {
                    assertEquals(0, bud.lean());
                } else {
                    assertTrue(bud.lean() >= CitrineTower.MAX_LEAN / 2 - 1e-6 && bud.lean() <= CitrineTower.MAX_LEAN + 1e-6,
                            "lean " + bud.lean());
                }
                assertTrue(Math.abs(bud.spin()) <= CitrineTower.MAX_LEAN + 1e-6);
            }
        }
    }

    @Test
    void budSizesStayWithinTheVariation() {
        for (int seed = 0; seed < CitrineTower.SEEDS; seed++) {
            for (Bud bud : full(seed).buds()) {
                assertTrue(bud.scale() >= 1 - CitrineTower.SIZE_VARIATION - 1e-6 && bud.scale() <= 1 + CitrineTower.SIZE_VARIATION + 1e-6);
            }
        }
    }

    @Test
    void aClosedSideGrowsNoBudsOnThatSideAndLevel() {
        Shape open = full(0);
        Shape closed = CitrineTower.grown(0, 1, (face, level) -> !(face == Face.EAST && level == 0));
        List<Bud> removed = open.buds().stream().filter(bud -> !closed.buds().contains(bud)).toList();
        assertEquals(1, removed.size());
        assertEquals(Face.EAST, removed.getFirst().face());
        assertEquals(0, (int) Math.floor(removed.getFirst().y() / 16));
    }

    @Test
    void sameSeedSameTowerOtherSeedOtherTower() {
        assertEquals(full(3), full(3));
        assertNotEquals(full(0), full(1));
        assertNotEquals(CitrineTower.seed(0, 0), CitrineTower.seed(1, 0));
    }

    @Test
    void smallBudsHaveThinnerShards() {
        Bud bud = full(0).buds().getFirst();
        Bud small = new Bud(bud.face(), bud.x(), bud.y(), bud.z(), 1, bud.lean(), bud.spin(), 1);
        Bud grown = new Bud(bud.face(), bud.x(), bud.y(), bud.z(), 4, bud.lean(), bud.spin(), 1);
        assertEquals(1, small.shards().getFirst().width());
        assertEquals(2, grown.shards().getFirst().width());
        assertEquals(6, grown.shards().getFirst().body() + grown.shards().getFirst().tipHeight());
    }
}
