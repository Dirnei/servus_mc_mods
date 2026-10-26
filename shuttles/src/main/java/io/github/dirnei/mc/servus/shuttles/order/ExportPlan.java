package io.github.dirnei.mc.servus.shuttles.order;

/** How much surplus of an item a stock rule's export ships, and whether its threshold may be saved. Pure, no Minecraft imports. */
public final class ExportPlan {

    private ExportPlan() {
    }

    /**
     * The surplus to request: the stock above the threshold, less what
     * requests still have to take out of that stock.
     *
     * @param stock     the item's count in the racks
     * @param threshold export above this
     * @param queued    items of requests that are still counted in {@code stock}
     */
    public static long surplus(long stock, int threshold, long queued) {
        return Math.max(0, stock - Math.max(0, threshold) - Math.max(0, queued));
    }

    /** The surplus bounded by the station's free slots, one stack per slot. */
    public static int bounded(long surplus, int freeSlots, int maxStackSize) {
        long room = (long) Math.max(0, freeSlots) * Math.max(1, maxStackSize);
        return (int) Math.max(0, Math.min(surplus, room));
    }

    /**
     * Whether an export threshold may be saved beside Fill up to: always
     * without Fill up to, else only above it.
     *
     * @param fillUpTo    the Make maximum, below 0 for no Make pair
     * @param exportAbove the export threshold
     */
    public static boolean validExport(int fillUpTo, int exportAbove) {
        return fillUpTo < 0 || exportAbove > fillUpTo;
    }
}
