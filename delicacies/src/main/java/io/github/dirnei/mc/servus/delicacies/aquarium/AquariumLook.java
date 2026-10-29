package io.github.dirnei.mc.servus.delicacies.aquarium;

/** How many fish an aquarium draws. Pure Java. */
public final class AquariumLook {

    private AquariumLook() {
    }

    /**
     * The cap times the fullness, rounded; at least one per whole fish up to two, never more than the cap or the
     * whole fish there are.
     */
    public static int drawnFish(double stock, int capacity, int cap) {
        int whole = (int) Math.floor(Math.max(0, stock));
        if (whole == 0 || cap <= 0) {
            return 0;
        }
        int byFullness = capacity <= 0 ? 0 : (int) Math.round(cap * Math.min(1, stock / capacity));
        int drawn = Math.max(byFullness, Math.min(whole, 2));
        return Math.min(Math.min(drawn, cap), whole);
    }

    /** The fullness in percent, 0 without capacity. */
    public static int fullnessPercent(double stock, int capacity) {
        return capacity <= 0 ? 0 : (int) Math.round(Math.min(1, Math.max(0, stock) / capacity) * 100);
    }
}
