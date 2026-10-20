package io.github.dirnei.mc.servus.shuttles.gantry;

/**
 * How much fluid one tanker run takes out of an Import gantry tank. Small
 * amounts wait until they reach the min load, unless the tank is full. Pure Java.
 */
public final class FluidPutAwayPlan {

    private FluidPutAwayPlan() {
    }

    /**
     * The amount of the run, 0 when no run should start.
     *
     * @param free            the tank's amount minus what runs already take from it
     * @param full            whether the tank is full
     * @param minLoad         the smallest run worth a trip
     * @param shuttleCapacity what a tanker carries at most
     * @param room            the room of the chosen target tank
     */
    public static int amount(int free, boolean full, int minLoad, int shuttleCapacity, int room) {
        if (free <= 0 || room <= 0 || shuttleCapacity <= 0) {
            return 0;
        }
        if (free < minLoad && !full) {
            return 0;
        }
        return Math.min(free, Math.min(shuttleCapacity, room));
    }
}
