package io.github.dirnei.mc.servus.logistics.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TankBoxesTest {

    /** Nothing missing can arrive, everything may merge. */
    private static final TankBoxes.Rules LOADED = new TankBoxes.Rules() {
        @Override
        public boolean mayArrive(NodePos pos) {
            return false;
        }

        @Override
        public boolean mayMerge(TankShape a, TankShape b) {
            return true;
        }
    };

    private static NodePos p(int x, int y, int z) {
        return new NodePos(x, y, z);
    }

    private static TankBoxes floor2x2() {
        TankBoxes boxes = new TankBoxes(LOADED);
        boxes.add(p(0, 0, 0), null);
        boxes.add(p(1, 0, 0), null);
        boxes.add(p(0, 0, 1), null);
        boxes.add(p(1, 0, 1), null);
        return boxes;
    }

    @Test
    void lShapeIsAPairAndASingleUntilTheFloorCompletes() {
        TankBoxes boxes = new TankBoxes(LOADED);
        boxes.add(p(0, 0, 0), null);
        boxes.add(p(1, 0, 0), null);
        boxes.add(p(0, 0, 1), null);
        assertEquals(TankShape.box(0, 0, 0, 1, 0, 0), boxes.boxOf(p(0, 0, 0)));
        assertEquals(TankShape.single(p(0, 0, 1)), boxes.boxOf(p(0, 0, 1)));

        boxes.add(p(1, 0, 1), null);
        assertEquals(Set.of(TankShape.box(0, 0, 0, 1, 0, 1)), boxes.boxes());
    }

    @Test
    void blockOnTopLeavesTheFloorFormed() {
        TankBoxes boxes = floor2x2();
        boxes.add(p(0, 1, 0), null);
        assertEquals(TankShape.box(0, 0, 0, 1, 0, 1), boxes.boxOf(p(1, 0, 1)));
        assertEquals(TankShape.single(p(0, 1, 0)), boxes.boxOf(p(0, 1, 0)));
    }

    @Test
    void completingTheTopLayerGrowsTheTank() {
        TankBoxes boxes = floor2x2();
        boxes.add(p(0, 1, 0), null);
        boxes.add(p(1, 1, 1), null);
        boxes.add(p(1, 1, 0), null);
        boxes.add(p(0, 1, 1), null);
        assertEquals(Set.of(TankShape.box(0, 0, 0, 1, 1, 1)), boxes.boxes());
    }

    @Test
    void blockBesideABoxStaysOnItsOwn() {
        TankBoxes boxes = floor2x2();
        boxes.add(p(2, 0, 0), null);
        assertEquals(TankShape.box(0, 0, 0, 1, 0, 1), boxes.boxOf(p(0, 0, 0)));
        assertEquals(TankShape.single(p(2, 0, 0)), boxes.boxOf(p(2, 0, 0)));
    }

    @Test
    void boxesWithMatchingFacesMerge() {
        TankBoxes boxes = floor2x2();
        boxes.add(p(2, 0, 0), null);
        boxes.add(p(3, 0, 0), null);
        boxes.add(p(2, 0, 1), null);
        boxes.add(p(3, 0, 1), null);
        assertEquals(Set.of(TankShape.box(0, 0, 0, 3, 0, 1)), boxes.boxes());
    }

    @Test
    void mergeCanBeRefused() {
        TankBoxes boxes = new TankBoxes(new TankBoxes.Rules() {
            @Override
            public boolean mayArrive(NodePos pos) {
                return false;
            }

            @Override
            public boolean mayMerge(TankShape a, TankShape b) {
                return false;
            }
        });
        boxes.add(p(0, 0, 0), null);
        boxes.add(p(1, 0, 0), null);
        assertEquals(2, boxes.boxes().size());
    }

    @Test
    void removingACornerRegroupsTheRest() {
        TankBoxes boxes = floor2x2();
        for (NodePos pos : TankShape.box(0, 1, 0, 1, 1, 1).positions()) {
            boxes.add(pos, null);
        }
        assertEquals(1, boxes.boxes().size());
        boxes.remove(p(1, 1, 1));
        assertEquals(7, boxes.size());
        assertFalse(boxes.contains(p(1, 1, 1)));
        int covered = 0;
        Set<NodePos> seen = new HashSet<>();
        for (TankShape box : boxes.boxes()) {
            for (NodePos pos : box.positions()) {
                assertTrue(seen.add(pos), "boxes overlap at " + pos);
                assertEquals(box, boxes.boxOf(pos));
                covered++;
            }
        }
        assertEquals(7, covered);
        assertTrue(boxes.boxes().size() > 1);
    }

    @Test
    void savedBoxWaitsForAllMembersThenRestores() {
        TankShape saved = TankShape.box(0, 0, 0, 1, 0, 1);
        TankBoxes boxes = new TankBoxes(new TankBoxes.Rules() {
            @Override
            public boolean mayArrive(NodePos pos) {
                return true;
            }

            @Override
            public boolean mayMerge(TankShape a, TankShape b) {
                return true;
            }
        });
        // The lone block on top arrives first and must not pull floor blocks into a column.
        boxes.add(p(0, 1, 0), null);
        boxes.add(p(0, 0, 0), saved);
        boxes.add(p(1, 0, 0), saved);
        assertTrue(boxes.isWaiting(p(0, 0, 0)));
        assertEquals(TankShape.single(p(0, 1, 0)), boxes.boxOf(p(0, 1, 0)));
        boxes.add(p(0, 0, 1), saved);
        boxes.add(p(1, 0, 1), saved);
        assertFalse(boxes.hasWaiting());
        assertEquals(saved, boxes.boxOf(p(1, 0, 1)));
        assertEquals(TankShape.single(p(0, 1, 0)), boxes.boxOf(p(0, 1, 0)));
    }

    @Test
    void waitingBlocksAreReleasedWhenAMemberCannotArrive() {
        TankShape saved = TankShape.box(0, 0, 0, 1, 0, 0);
        TankBoxes boxes = new TankBoxes(LOADED);
        boxes.add(p(0, 0, 0), saved);
        assertFalse(boxes.hasWaiting());
        assertEquals(TankShape.single(p(0, 0, 0)), boxes.boxOf(p(0, 0, 0)));
    }

    @Test
    void absorbKeepsBothSidesBoxes() {
        TankBoxes left = floor2x2();
        TankBoxes right = new TankBoxes(LOADED);
        right.add(p(0, 1, 0), null);
        left.absorb(right);
        assertEquals(TankShape.box(0, 0, 0, 1, 0, 1), left.boxOf(p(0, 0, 0)));
        assertEquals(TankShape.single(p(0, 1, 0)), left.boxOf(p(0, 1, 0)));
    }

    @Test
    void mergeWithNeedsMatchingFaces() {
        TankShape floor = TankShape.box(0, 0, 0, 1, 0, 1);
        assertEquals(TankShape.box(0, 0, 0, 1, 1, 1), floor.mergeWith(TankShape.box(0, 1, 0, 1, 1, 1)).orElseThrow());
        assertTrue(floor.mergeWith(TankShape.single(p(0, 1, 0))).isEmpty());
        assertTrue(floor.mergeWith(TankShape.box(0, 2, 0, 1, 2, 1)).isEmpty());
    }
}
