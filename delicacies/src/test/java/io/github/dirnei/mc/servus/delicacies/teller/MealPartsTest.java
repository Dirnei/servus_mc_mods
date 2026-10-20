package io.github.dirnei.mc.servus.delicacies.teller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.delicacies.teller.MealPart.Face;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MealPartsTest {

    private static final float EPSILON = 1e-4F;

    @Test
    void eachPortionShowsOnePartMore() {
        for (MealKind kind : MealKind.values()) {
            for (int left = 1; left <= MealPortions.FULL; left++) {
                List<MealPart> food = MealParts.food(kind, left);
                assertEquals(kind.lasting().size() + left, food.size(), kind + " with " + left);
                assertTrue(food.containsAll(kind.lasting()), kind + " keeps its base with " + left);
                assertTrue(food.containsAll(kind.portions().subList(0, left)), kind + " shows its first portions");
            }
        }
    }

    @Test
    void theLastListedPortionIsEatenFirst() {
        MealKind kind = MealKind.ROAST_PORK_PLATE;
        List<MealPart> full = MealParts.food(kind, MealPortions.FULL);
        List<MealPart> eaten = MealParts.food(kind, MealPortions.afterEating(MealPortions.FULL));
        assertTrue(full.contains(kind.portions().get(MealPortions.FULL - 1)));
        assertFalse(eaten.contains(kind.portions().get(MealPortions.FULL - 1)));
    }

    @Test
    void outOfRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> MealParts.food(MealKind.SCHNITZEL_PLATE, 0));
    }

    @Test
    void foodRestsOnThePlateInsideTheBlock() {
        for (MealKind kind : MealKind.values()) {
            List<MealPart> food = MealParts.food(kind, MealPortions.FULL);
            for (MealPart part : food) {
                float[] b = part.box();
                assertTrue(b[0] >= 1 && b[3] <= 15 && b[2] >= 1 && b[5] <= 15 && b[4] <= 16, kind + " part inside the plate");
                assertTrue(b[0] < b[3] && b[1] < b[4] && b[2] < b[5], kind + " part has a volume");
                boolean onPlate = Math.abs(b[1] - MealParts.PLATE_TOP) < EPSILON;
                boolean onBase = kind.lasting().stream().anyMatch(base -> Math.abs(base.box()[4] - b[1]) < EPSILON);
                assertTrue(onPlate || onBase, kind + " part rests on the plate or the base");
                assertFalse(part.shows(Face.DOWN), kind + " food has no down face");
            }
        }
    }

    @Test
    void noTwoFacesShareAPlaneWhereTheyOverlap() {
        List<List<MealPart>> states = new ArrayList<>();
        states.add(MealParts.PLATE);
        for (MealKind kind : MealKind.values()) {
            for (int left = 1; left <= MealPortions.FULL; left++) {
                List<MealPart> parts = new ArrayList<>(MealParts.PLATE);
                parts.addAll(MealParts.food(kind, left));
                states.add(parts);
            }
        }
        for (List<MealPart> parts : states) {
            for (int i = 0; i < parts.size(); i++) {
                for (int j = i + 1; j < parts.size(); j++) {
                    assertNoSharedPlane(parts.get(i), parts.get(j));
                }
            }
        }
    }

    private static void assertNoSharedPlane(MealPart a, MealPart b) {
        for (Face fa : Face.values()) {
            for (Face fb : Face.values()) {
                if (a.shows(fa) && b.shows(fb) && axis(fa) == axis(fb)
                        && Math.abs(plane(a, fa) - plane(b, fb)) < EPSILON && overlapOn(a, b, axis(fa))) {
                    throw new AssertionError("faces " + fa + " of " + describe(a) + " and " + fb + " of " + describe(b)
                            + " share a plane");
                }
            }
        }
    }

    private static int axis(Face face) {
        return switch (face) {
            case WEST, EAST -> 0;
            case DOWN, UP -> 1;
            case NORTH, SOUTH -> 2;
        };
    }

    private static float plane(MealPart part, Face face) {
        float[] b = part.box();
        return switch (face) {
            case WEST -> b[0];
            case EAST -> b[3];
            case DOWN -> b[1];
            case UP -> b[4];
            case NORTH -> b[2];
            case SOUTH -> b[5];
        };
    }

    /** Whether the two boxes' faces normal to {@code axis} overlap with an area. */
    private static boolean overlapOn(MealPart a, MealPart b, int axis) {
        for (int other = 0; other < 3; other++) {
            if (other == axis) {
                continue;
            }
            float low = Math.max(a.box()[other], b.box()[other]);
            float high = Math.min(a.box()[other + 3], b.box()[other + 3]);
            if (high - low < EPSILON) {
                return false;
            }
        }
        return true;
    }

    private static String describe(MealPart part) {
        return part.top() + Arrays.toString(part.box());
    }
}
