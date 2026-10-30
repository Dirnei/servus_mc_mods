package io.github.dirnei.mc.servus.shuttles.rack;

/**
 * Whether a newly placed bay joins the shelf beside it. Shelves hold at
 * most {@code max} bays and never change once built. Pure Java.
 */
public final class RackRow {

    private RackRow() {
    }

    /**
     * @param leftSize bays in the shelf on the left, 0 when no rack is there
     * @param max      bays a shelf may hold
     */
    public static boolean joinLeft(int leftSize, int max) {
        return leftSize > 0 && leftSize < Math.max(1, max);
    }

    /**
     * @param leftJoined whether the new bay joined the left shelf
     * @param leftSize   bays in the shelf on the left, 0 when no rack is there
     * @param rightSize  bays in the shelf on the right, 0 when no rack is there
     * @param max        bays a shelf may hold
     */
    public static boolean joinRight(boolean leftJoined, int leftSize, int rightSize, int max) {
        int own = leftJoined ? leftSize + 1 : 1;
        return rightSize > 0 && own + rightSize <= Math.max(1, max);
    }
}
