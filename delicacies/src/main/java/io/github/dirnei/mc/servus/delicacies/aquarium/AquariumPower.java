package io.github.dirnei.mc.servus.delicacies.aquarium;

/** The power an aquarium draws. Pure Java. */
public final class AquariumPower {

    private AquariumPower() {
    }

    /** FE per tick for {@code tankBlocks} tank blocks under {@code lidSlabs} lid slabs. */
    public static int cost(int tankBlocks, int lidSlabs, AquariumStats stats) {
        return tankBlocks * stats.fePerTankBlock() + lidSlabs * stats.fePerLid();
    }

    /** The FE buffer of an aquarium with that cost: enough for the data's buffer ticks, at least 1. */
    public static int buffer(int cost, AquariumStats stats) {
        return Math.max(1, cost * stats.energyBufferTicks());
    }

    /** The share of {@code ticks} that had power, 0 when there were none. */
    public static double share(int poweredTicks, int ticks) {
        return ticks <= 0 ? 0 : Math.min(1, (double) poweredTicks / ticks);
    }
}
