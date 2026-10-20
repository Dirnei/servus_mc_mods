package io.github.dirnei.mc.servus.delicacies.teller;

import java.util.Set;

/**
 * One box of a plate or a meal in pixels, built facing north: {@code x0, y0, z0, x1, y1, z1}, the block textures of its
 * top and of its other faces, and the faces it leaves out.
 */
public record MealPart(float[] box, String top, String side, Set<Face> hidden) {

    /** The faces of a box, named like the block directions. */
    public enum Face {
        DOWN, UP, NORTH, SOUTH, WEST, EAST
    }

    /** Food resting on the plate or on a base part: no down face. */
    static MealPart food(String top, String side, float... box) {
        return new MealPart(box, top, side, Set.of(Face.DOWN));
    }

    /** Food with one texture on every face. */
    static MealPart food(String texture, float... box) {
        return food(texture, texture, box);
    }

    /** Whether the face is drawn. */
    public boolean shows(Face face) {
        return !hidden.contains(face);
    }
}
