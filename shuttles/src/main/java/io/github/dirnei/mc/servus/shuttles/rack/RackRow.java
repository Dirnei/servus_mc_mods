package io.github.dirnei.mc.servus.shuttles.rack;

/**
 * Whether a newly placed bay joins the section beside it. Sections hold at
 * most {@code max} bays and never change once built. Pure Java.
 */
public final class RackRow {

    private RackRow() {
    }

    /**
     * @param leftSize bays in the section on the left, 0 when no rack is there
     * @param max      bays a section may hold
     */
    public static boolean joinLeft(int leftSize, int max) {
        return leftSize > 0 && leftSize < Math.max(1, max);
    }

    /**
     * @param leftJoined whether the new bay joined the left section
     * @param leftSize   bays in the section on the left, 0 when no rack is there
     * @param rightSize  bays in the section on the right, 0 when no rack is there
     * @param max        bays a section may hold
     */
    public static boolean joinRight(boolean leftJoined, int leftSize, int rightSize, int max) {
        int own = leftJoined ? leftSize + 1 : 1;
        return rightSize > 0 && own + rightSize <= Math.max(1, max);
    }
}
