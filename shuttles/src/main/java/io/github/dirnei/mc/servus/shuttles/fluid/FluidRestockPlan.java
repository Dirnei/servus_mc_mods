package io.github.dirnei.mc.servus.shuttles.fluid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * How a tanker restocks a kept Fluid Outbound tank: how much is missing, how
 * much one run brings and which fluid store it comes from. Pure Java;
 * {@code T} names a store.
 */
public final class FluidRestockPlan {

    /**
     * One fluid store holding the fluid: a Fluid Rack shelf.
     *
     * @param id       names the tank
     * @param free     its amount minus what runs already take from it
     * @param distance how far it is from the kept tank, in rail steps
     */
    public record Source<T>(T id, int free, int distance) {
    }

    /** One bucket in mB; restock runs carry whole buckets only. */
    public static final int BUCKET = 1000;

    private FluidRestockPlan() {
    }

    /** What the kept tank lacks once every run on its way arrived, 0 when nothing does. */
    public static int missing(int keep, int amount, int incoming) {
        return Math.max(0, keep - amount - Math.max(0, incoming));
    }

    /**
     * Whether the kept tank restocks: it starts once its have is at or below
     * the restock level and goes on while at least one bucket is missing to
     * the keep amount.
     *
     * @param was       whether it restocked at the last check
     * @param have      its amount plus what runs on the way bring
     * @param restockAt the restock level
     * @param keep      the keep amount
     */
    public static boolean restocking(boolean was, int have, int restockAt, int keep) {
        if (keep <= 0) {
            return false;
        }
        return have <= restockAt || was && keep - have >= BUCKET;
    }

    /**
     * The amount of the run in whole buckets, 0 when no run should start. A
     * run below the min load waits unless it brings every whole bucket missing.
     *
     * @param missing         what the kept tank lacks
     * @param minLoad         the smallest run worth a trip
     * @param shuttleCapacity what a tanker carries at most
     * @param sourceFree      what the source tank holds beyond the runs already taking from it
     */
    public static int amount(int missing, int minLoad, int shuttleCapacity, int sourceFree) {
        if (missing <= 0 || shuttleCapacity <= 0 || sourceFree <= 0) {
            return 0;
        }
        int amount = wholeBuckets(Math.min(missing, Math.min(shuttleCapacity, sourceFree)));
        return amount < minLoad && amount < wholeBuckets(missing) ? 0 : amount;
    }

    private static int wholeBuckets(int amount) {
        return amount / BUCKET * BUCKET;
    }

    /** The sources with free fluid, the one with the most first, then the nearer one. */
    public static <T> List<Source<T>> sources(List<Source<T>> candidates) {
        List<Source<T>> sources = new ArrayList<>();
        for (Source<T> candidate : candidates) {
            if (candidate.free() > 0) {
                sources.add(candidate);
            }
        }
        sources.sort(Comparator.<Source<T>>comparingInt(Source::free).reversed().thenComparingInt(Source::distance));
        return sources;
    }
}
