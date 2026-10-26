package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class TreeWalkTest {

    private static final int BASE_Y = 1;

    private final Map<FieldPos, TreeWalk.Kind> world = new HashMap<>();

    private void put(int x, int y, int z, TreeWalk.Kind kind) {
        world.put(new FieldPos(x, y, z), kind);
    }

    /** An oak: a trunk of {@code height} logs and a crown of leaves two wide around its top two logs and one above. */
    private void oak(int x, int z, int height) {
        int top = BASE_Y + height - 1;
        for (int y = top - 1; y <= top + 1; y++) {
            int radius = y > top ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    put(x + dx, y, z + dz, TreeWalk.Kind.NATURAL_LEAVES);
                }
            }
        }
        for (int y = BASE_Y; y <= top; y++) {
            put(x, y, z, TreeWalk.Kind.LOG);
        }
    }

    private TreeWalk.Kind kind(FieldPos pos) {
        return world.getOrDefault(pos, TreeWalk.Kind.OTHER);
    }

    private TreeWalk.Result walk(List<FieldPos> base, Predicate<FieldPos> otherBase, int maxLogs, int maxBlocks) {
        return TreeWalk.walk(base, this::kind, otherBase, maxLogs, maxBlocks);
    }

    private TreeWalk.Felled fell(int x, int z) {
        return assertInstanceOf(TreeWalk.Felled.class, walk(List.of(new FieldPos(x, BASE_Y, z)), pos -> false, 200, 1000));
    }

    @Test
    void oakComesDownWhole() {
        oak(0, 0, 5);
        TreeWalk.Felled felled = fell(0, 0);
        assertEquals(5, felled.logs().size());
        long leaves = world.values().stream().filter(kind -> kind == TreeWalk.Kind.NATURAL_LEAVES).count();
        assertEquals(leaves, felled.leaves().size(), "every leaf of a lone oak goes");
        assertTrue(felled.attachments().isEmpty());
    }

    @Test
    void touchingCrownsKeepTheNeighboursLeaves() {
        oak(0, 0, 5);
        oak(4, 0, 5);
        TreeWalk.Felled felled = fell(0, 0);
        assertEquals(5, felled.logs().size());
        assertFalse(felled.logs().contains(new FieldPos(4, BASE_Y, 0)), "the neighbour's trunk stays");
        for (FieldPos leaf : felled.leaves()) {
            assertTrue(leaf.x() < 2, "no leaf the neighbour holds goes: " + leaf);
        }
        assertTrue(felled.leaves().contains(new FieldPos(-2, 4, 0)), "leaves on the far side go");
        assertFalse(felled.leaves().contains(new FieldPos(2, 4, 0)), "a shared leaf next to both trunks stays");
    }

    @Test
    void diagonalBranchComesAlong() {
        put(0, 1, 0, TreeWalk.Kind.LOG);
        put(0, 2, 0, TreeWalk.Kind.LOG);
        put(1, 3, 1, TreeWalk.Kind.LOG);
        put(2, 4, 2, TreeWalk.Kind.LOG);
        put(2, 5, 2, TreeWalk.Kind.NATURAL_LEAVES);
        TreeWalk.Felled felled = fell(0, 0);
        assertEquals(4, felled.logs().size());
        assertEquals(Set.of(new FieldPos(2, 5, 2)), felled.leaves());
    }

    @Test
    void squareBaseFellsTheWholeTrunk() {
        List<FieldPos> base = TreeSpots.members(new FieldPos(0, BASE_Y, 0));
        for (FieldPos plot : base) {
            for (int y = BASE_Y; y <= 4; y++) {
                put(plot.x(), y, plot.z(), TreeWalk.Kind.LOG);
            }
        }
        put(0, 5, 0, TreeWalk.Kind.NATURAL_LEAVES);
        TreeWalk.Felled felled = assertInstanceOf(TreeWalk.Felled.class, walk(base, pos -> false, 200, 1000));
        assertEquals(16, felled.logs().size());
    }

    @Test
    void neighbourTrunkOnTheBaseLevelStays() {
        oak(0, 0, 5);
        put(1, BASE_Y, 1, TreeWalk.Kind.LOG);
        put(1, BASE_Y + 1, 1, TreeWalk.Kind.LOG);
        TreeWalk.Felled felled = assertInstanceOf(TreeWalk.Felled.class,
                walk(List.of(new FieldPos(0, BASE_Y, 0)), pos -> pos.x() == 1 && pos.z() == 1, 200, 1000));
        assertFalse(felled.logs().contains(new FieldPos(1, BASE_Y, 1)), "a log on another base is never entered");
        assertFalse(felled.logs().contains(new FieldPos(1, BASE_Y + 1, 1)), "nor the trunk above it");
    }

    @Test
    void branchTouchingTheNeighboursTrunkLeavesItWhole() {
        oak(0, 0, 5);
        for (int y = BASE_Y; y < BASE_Y + 5; y++) {
            put(2, y, 0, TreeWalk.Kind.LOG);
        }
        put(1, 3, 0, TreeWalk.Kind.LOG);
        Predicate<FieldPos> neighbourBase = pos -> pos.x() == 2 && pos.z() == 0;
        TreeWalk.Felled felled = assertInstanceOf(TreeWalk.Felled.class, walk(List.of(new FieldPos(0, BASE_Y, 0)), neighbourBase, 200, 1000));
        for (int y = BASE_Y; y < BASE_Y + 5; y++) {
            assertTrue(felled.logs().contains(new FieldPos(0, y, 0)), "the own trunk goes at " + y);
            assertFalse(felled.logs().contains(new FieldPos(2, y, 0)), "the neighbour's trunk stays at " + y);
        }
    }

    @Test
    void neighbourKeepsTheLeanThatLeadsToItsCrown() {
        for (int y = BASE_Y; y <= 3; y++) {
            put(0, y, 0, TreeWalk.Kind.LOG);
        }
        put(2, 1, 0, TreeWalk.Kind.LOG);
        put(2, 2, 0, TreeWalk.Kind.LOG);
        put(1, 3, 0, TreeWalk.Kind.LOG);
        put(1, 4, 0, TreeWalk.Kind.LOG);
        put(1, 5, 0, TreeWalk.Kind.NATURAL_LEAVES);
        put(0, 4, 0, TreeWalk.Kind.NATURAL_LEAVES);
        TreeWalk.Felled felled = assertInstanceOf(TreeWalk.Felled.class,
                walk(List.of(new FieldPos(0, BASE_Y, 0)), pos -> pos.x() == 2 && pos.z() == 0, 200, 1000));
        assertEquals(3, felled.logs().size(), "only the own straight trunk goes, got " + felled.logs());
        assertFalse(felled.logs().contains(new FieldPos(1, 4, 0)), "the lean the neighbour climbs to its crown stays");
    }

    @Test
    void trunkWithoutLeavesTouchingAnotherTreeIsFelled() {
        for (int y = BASE_Y; y <= 3; y++) {
            put(0, y, 0, TreeWalk.Kind.LOG);
        }
        oak(2, 0, 5);
        TreeWalk.Felled felled = assertInstanceOf(TreeWalk.Felled.class,
                walk(List.of(new FieldPos(0, BASE_Y, 0)), pos -> pos.x() == 2 && pos.z() == 0, 200, 1000));
        assertEquals(3, felled.logs().size());
        assertTrue(felled.leaves().isEmpty(), "the neighbour keeps its leaves");
    }

    @Test
    void trunkBelowTheBaseComesDown() {
        oak(0, 0, 5);
        put(0, BASE_Y - 1, 0, TreeWalk.Kind.LOG);
        put(0, BASE_Y - 2, 0, TreeWalk.Kind.LOG);
        TreeWalk.Felled felled = fell(0, 0);
        assertEquals(7, felled.logs().size());
        assertTrue(felled.logs().contains(new FieldPos(0, BASE_Y - 2, 0)), "the trunk goes down to the ground");
    }

    @Test
    void logLimit() {
        oak(0, 0, 8);
        assertInstanceOf(TreeWalk.TooLarge.class, walk(List.of(new FieldPos(0, BASE_Y, 0)), pos -> false, 7, 1000));
        assertInstanceOf(TreeWalk.Felled.class, walk(List.of(new FieldPos(0, BASE_Y, 0)), pos -> false, 8, 1000));
    }

    @Test
    void blockLimit() {
        oak(0, 0, 5);
        assertInstanceOf(TreeWalk.TooLarge.class, walk(List.of(new FieldPos(0, BASE_Y, 0)), pos -> false, 200, 20));
    }

    @Test
    void logPillarIsNoTree() {
        for (int y = BASE_Y; y < BASE_Y + 3; y++) {
            put(0, y, 0, TreeWalk.Kind.LOG);
        }
        assertInstanceOf(TreeWalk.NotATree.class, walk(List.of(new FieldPos(0, BASE_Y, 0)), pos -> false, 200, 1000));
        assertInstanceOf(TreeWalk.NotATree.class, walk(List.of(new FieldPos(5, BASE_Y, 5)), pos -> false, 200, 1000));
    }

    @Test
    void vinesHangingFromTheTreeGo() {
        oak(0, 0, 5);
        put(1, 2, 0, TreeWalk.Kind.ATTACHMENT);
        put(-3, 4, 0, TreeWalk.Kind.ATTACHMENT);
        put(-3, 3, 0, TreeWalk.Kind.ATTACHMENT);
        put(-3, 2, 0, TreeWalk.Kind.ATTACHMENT);
        put(6, 2, 6, TreeWalk.Kind.ATTACHMENT);
        TreeWalk.Felled felled = fell(0, 0);
        assertEquals(Set.of(new FieldPos(1, 2, 0), new FieldPos(-3, 4, 0), new FieldPos(-3, 3, 0), new FieldPos(-3, 2, 0)),
                felled.attachments(), "the vine column hangs on, the far vine stays");
    }
}
