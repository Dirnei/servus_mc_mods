package io.github.dirnei.mc.servus.storage.rack;

/**
 * Where posts stand in a row of joined bays: at both ends and after every
 * {@code postsEvery} bays counted from the left. Pure, no Minecraft imports.
 */
public final class RackRow {

    private RackRow() {
    }

    /** Whether the bay at this index shares a post free edge with the bay to its left. */
    public static boolean leftPostHidden(int bayIndex, int postsEvery) {
        int every = Math.max(1, postsEvery);
        return bayIndex > 0 && bayIndex % every != 0;
    }

    /** Whether the bay at this index shares a post free edge with the bay to its right. */
    public static boolean rightPostHidden(int bayIndex, int bayCount, int postsEvery) {
        int every = Math.max(1, postsEvery);
        return bayIndex < bayCount - 1 && (bayIndex + 1) % every != 0;
    }
}
