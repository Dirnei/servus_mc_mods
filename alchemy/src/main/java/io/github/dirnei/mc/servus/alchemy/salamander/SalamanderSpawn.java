package io.github.dirnei.mc.servus.alchemy.salamander;

/** The pure crowd check of a wild salamander's spawn. */
public final class SalamanderSpawn {

    private SalamanderSpawn() {
    }

    /** Whether one more wild salamander may spawn where {@code wildNearby} already live, with a limit of {@code max}. */
    public static boolean crowdAllows(int wildNearby, int max) {
        return wildNearby < max;
    }
}
