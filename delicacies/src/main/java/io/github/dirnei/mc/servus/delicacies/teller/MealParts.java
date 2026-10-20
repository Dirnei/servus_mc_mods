package io.github.dirnei.mc.servus.delicacies.teller;

import io.github.dirnei.mc.servus.delicacies.teller.MealPart.Face;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The boxes of the plate and of a meal in each portion state, in pixels, built facing north. The plate is a stepped
 * round plate {@link #PLATE_TOP} pixel high; the pieces meet back to back and leave out the face toward their inner
 * neighbour.
 */
public final class MealParts {

    /** Height of the plate's top face, where food rests. */
    public static final float PLATE_TOP = 1;

    private static final String TOP = "plate";
    private static final String RIM = "plate_ceramic";

    /** The plate: a centre band, two middle steps and two end steps. */
    public static final List<MealPart> PLATE = List.of(
            new MealPart(new float[] {1, 0, 4, 15, PLATE_TOP, 12}, TOP, RIM, Set.of()),
            new MealPart(new float[] {2, 0, 2, 14, PLATE_TOP, 4}, TOP, RIM, Set.of(Face.SOUTH)),
            new MealPart(new float[] {2, 0, 12, 14, PLATE_TOP, 14}, TOP, RIM, Set.of(Face.NORTH)),
            new MealPart(new float[] {4, 0, 1, 12, PLATE_TOP, 2}, TOP, RIM, Set.of(Face.SOUTH)),
            new MealPart(new float[] {4, 0, 14, 12, PLATE_TOP, 15}, TOP, RIM, Set.of(Face.NORTH)));

    private MealParts() {
    }

    /** The food of {@code kind} with {@code left} portions: the lasting parts and the first {@code left} portion parts. */
    public static List<MealPart> food(MealKind kind, int left) {
        MealPortions.check(left);
        List<MealPart> parts = new ArrayList<>(kind.lasting());
        parts.addAll(kind.portions().subList(0, left));
        return List.copyOf(parts);
    }

    /** Every box of {@code kind} with {@code left} portions, the plate first. */
    public static float[][] boxes(MealKind kind, int left) {
        List<MealPart> parts = new ArrayList<>(PLATE);
        parts.addAll(food(kind, left));
        return boxesOf(parts);
    }

    /** The boxes of the empty plate. */
    public static float[][] plateBoxes() {
        return boxesOf(PLATE);
    }

    private static float[][] boxesOf(List<MealPart> parts) {
        return parts.stream().map(MealPart::box).toArray(float[][]::new);
    }
}
