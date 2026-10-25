package io.github.dirnei.mc.servus.shuttles.field;

/** How many crop items or saplings a field shuttle takes from the head's buffer before it works the field. Pure Java. */
public final class SeedPlan {

    /** Saplings a square of a 2x2 field takes at once. */
    public static final int SQUARE = 4;

    private SeedPlan() {
    }

    /**
     * The harvest comes first: nothing is fetched while ripe plots remain.
     * Otherwise the shortfall of crop items for the plots to plant, at most
     * one stack, what the warehouse holds and what the cargo has room for.
     *
     * @param ripe      ripe plots left on the field
     * @param toPlant   plots to plant, tilled ones included
     * @param carried   crop items the harvester carries
     * @param available crop items the warehouse holds
     * @param room      crop items the cargo can still take
     * @param stackSize the crop item's stack size
     */
    public static int toFetch(int ripe, int toPlant, int carried, long available, int room, int stackSize) {
        return toFetch(ripe, toPlant, carried, available, room, stackSize, 1);
    }

    /**
     * As {@link #toFetch(int, int, int, long, int, int)}, in whole groups of
     * {@code group} items counted with what is carried: a 2x2 field plants
     * four saplings per square at once, so it fetches no odd ones.
     */
    public static int toFetch(int ripe, int toPlant, int carried, long available, int room, int stackSize, int group) {
        if (ripe > 0) {
            return 0;
        }
        long wanted = Math.max(0, Math.min(Math.min(toPlant - carried, stackSize), Math.min(available, room)));
        int step = Math.max(1, group);
        long whole = (carried + wanted) / step * step - carried;
        return (int) Math.max(0, whole);
    }
}
