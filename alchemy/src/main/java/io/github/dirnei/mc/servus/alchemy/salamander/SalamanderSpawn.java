package io.github.dirnei.mc.servus.alchemy.salamander;

/** The pure checks of a wild salamander's spawn: a home block near enough and no crowd around. */
public final class SalamanderSpawn {

    private SalamanderSpawn() {
    }

    /** Whether one more wild salamander may spawn where {@code wildNearby} already live, with a limit of {@code max}. */
    public static boolean crowdAllows(int wildNearby, int max) {
        return wildNearby < max;
    }

    /** Whether a home block at the offset {@code dx, dy, dz} from the spawn spot lies within the box of {@code radius}. */
    public static boolean homeWithin(int dx, int dy, int dz, int radius) {
        return Math.abs(dx) <= radius && Math.abs(dy) <= radius && Math.abs(dz) <= radius;
    }
}
