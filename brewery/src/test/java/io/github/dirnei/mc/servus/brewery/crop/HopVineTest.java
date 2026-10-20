package io.github.dirnei.mc.servus.brewery.crop;

import static io.github.dirnei.mc.servus.brewery.crop.HopStage.BARE;
import static io.github.dirnei.mc.servus.brewery.crop.HopStage.CONES;
import static io.github.dirnei.mc.servus.brewery.crop.HopStage.GROWN;
import static io.github.dirnei.mc.servus.brewery.crop.HopStage.YOUNG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HopVineTest {

    @Test
    void youngPartLeafsOut() {
        assertEquals(Optional.of(new HopStep(0, GROWN)), HopVine.growthAt(List.of(YOUNG, BARE), 0));
    }

    @Test
    void grownPartClimbsOntoBarePoleAbove() {
        assertEquals(Optional.of(new HopStep(1, YOUNG)), HopVine.growthAt(List.of(GROWN, BARE, BARE), 0));
    }

    @Test
    void conesPartAlsoClimbs() {
        assertEquals(Optional.of(new HopStep(2, YOUNG)), HopVine.growthAt(List.of(GROWN, CONES, BARE), 1));
    }

    @Test
    void noConesBeforeTheTopHasLeafedOut() {
        assertEquals(Optional.empty(), HopVine.growthAt(List.of(GROWN, GROWN, YOUNG), 1));
    }

    @Test
    void upperPartsGrowConesOnceTheTopHasLeafedOut() {
        List<HopStage> column = List.of(GROWN, GROWN, GROWN);
        assertEquals(Optional.of(new HopStep(1, CONES)), HopVine.growthAt(column, 1));
        assertEquals(Optional.of(new HopStep(2, CONES)), HopVine.growthAt(column, 2));
    }

    @Test
    void middlePartGrowsConesWhenTheTopAlreadyHasCones() {
        assertEquals(Optional.of(new HopStep(1, CONES)), HopVine.growthAt(List.of(GROWN, GROWN, CONES), 1));
    }

    @Test
    void footNeverBearsCones() {
        assertEquals(Optional.empty(), HopVine.growthAt(List.of(GROWN, CONES), 0));
    }

    @Test
    void singlePoleNeverBearsCones() {
        assertEquals(Optional.empty(), HopVine.growthAt(List.of(GROWN), 0));
        assertEquals(Optional.empty(), HopVine.firstGrowth(List.of(GROWN)));
    }

    @Test
    void bareNeverGrows() {
        assertEquals(Optional.empty(), HopVine.growthAt(List.of(BARE, BARE), 0));
        assertEquals(Optional.empty(), HopVine.firstGrowth(List.of(BARE, BARE, BARE)));
    }

    @Test
    void climbComesBeforeCones() {
        assertEquals(Optional.of(new HopStep(2, YOUNG)), HopVine.growthAt(List.of(GROWN, GROWN, BARE), 1));
    }

    @Test
    void firstGrowthStartsAtTheFoot() {
        assertEquals(Optional.of(new HopStep(0, GROWN)), HopVine.firstGrowth(List.of(YOUNG, BARE, BARE)));
        assertEquals(Optional.of(new HopStep(1, CONES)), HopVine.firstGrowth(List.of(GROWN, GROWN, GROWN)));
    }

    @Test
    void firstGrowthIsEmptyWhenFullyGrown() {
        assertEquals(Optional.empty(), HopVine.firstGrowth(List.of(GROWN, CONES, CONES)));
    }

    @Test
    void repeatedFirstGrowthCoversAThreePoleColumn() {
        List<HopStage> column = new ArrayList<>(List.of(YOUNG, BARE, BARE));
        int steps = 0;
        for (Optional<HopStep> step = HopVine.firstGrowth(column); step.isPresent(); step = HopVine.firstGrowth(column)) {
            column.set(step.get().part(), step.get().stage());
            steps++;
        }
        assertEquals(List.of(GROWN, CONES, CONES), column);
        assertEquals(7, steps);
    }

    @Test
    void fourthPoleIsRefused() {
        assertTrue(HopVine.canPlaceOn(0));
        assertTrue(HopVine.canPlaceOn(2));
        assertFalse(HopVine.canPlaceOn(3));
    }
}
