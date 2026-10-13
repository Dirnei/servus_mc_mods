package io.github.dirnei.mc.servus.storage.field;

/** How many crop items a harvester picks from the warehouse before it drives out. Pure Java. */
public final class SeedPlan {

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
        if (ripe > 0) {
            return 0;
        }
        long wanted = Math.min(Math.min(toPlant - carried, stackSize), Math.min(available, room));
        return (int) Math.max(0, wanted);
    }
}
