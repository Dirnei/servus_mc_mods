package io.github.dirnei.mc.servus.alchemy.hint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Picks the wall block that becomes the old gnome's Ancient Hollow near a broken block. Pure Java: it walks a fixed,
 * ordered list of offsets and asks the caller what stands there, so nothing scans the world.
 */
public final class AncientHollowSite {

    /** Horizontal sides in clockwise order: north, east, south, west, as x and z steps. */
    public static final int[][] SIDES = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    private static final int[] DISTANCES = {3, 2, 4};
    private static final int[] SHIFTS = {1, -1, 0};
    private static final int[] HEIGHTS = {0, 1, -1};

    /** What stands at an offset from the broken block. */
    @FunctionalInterface
    public interface Probe {

        boolean test(int dx, int dy, int dz);
    }

    /**
     * The chosen wall block.
     *
     * @param dx   offset from the broken block
     * @param dy   offset from the broken block
     * @param dz   offset from the broken block
     * @param side index into {@link #SIDES} of the side with air, the Hollow's open face
     */
    public record Site(int dx, int dy, int dz, int side) {
    }

    private AncientHollowSite() {
    }

    /** Every offset checked, in order, for the given start direction (index into {@link #SIDES}). */
    public static List<int[]> candidates(int startDirection) {
        List<int[]> offsets = new ArrayList<>();
        for (int distance : DISTANCES) {
            for (int turn = 0; turn < SIDES.length; turn++) {
                int[] along = SIDES[Math.floorMod(startDirection + turn, SIDES.length)];
                int[] across = {-along[1], along[0]};
                for (int shift : SHIFTS) {
                    for (int height : HEIGHTS) {
                        offsets.add(new int[] {along[0] * distance + across[0] * shift, height, along[1] * distance + across[1] * shift});
                    }
                }
            }
        }
        return offsets;
    }

    /** The first candidate that is a wall with air on a horizontal side, or empty when none fits. */
    public static Optional<Site> choose(int startDirection, Probe isWall, Probe isAir) {
        for (int[] offset : candidates(startDirection)) {
            if (!isWall.test(offset[0], offset[1], offset[2])) {
                continue;
            }
            for (int side = 0; side < SIDES.length; side++) {
                if (isAir.test(offset[0] + SIDES[side][0], offset[1], offset[2] + SIDES[side][1])) {
                    return Optional.of(new Site(offset[0], offset[1], offset[2], side));
                }
            }
        }
        return Optional.empty();
    }
}
