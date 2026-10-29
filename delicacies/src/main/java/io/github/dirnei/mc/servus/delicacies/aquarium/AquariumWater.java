package io.github.dirnei.mc.servus.delicacies.aquarium;

/** Water exchange through an aquarium part. Pure Java. */
public final class AquariumWater {

    private AquariumWater() {
    }

    /** The dirt that leaves with {@code drained} of {@code water} mB: the same share as the water, all of it when it all goes. */
    public static double dirtTaken(double dirt, long drained, long water) {
        if (dirt <= 0 || drained <= 0) {
            return 0;
        }
        if (water <= drained) {
            return dirt;
        }
        return dirt * drained / water;
    }
}
