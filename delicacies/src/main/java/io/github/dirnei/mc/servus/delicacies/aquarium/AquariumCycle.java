package io.github.dirnei.mc.servus.delicacies.aquarium;

/**
 * One cycle of an aquarium: feeding, dirt, the filter, quality, deaths, growth and the catch, in that order. Pure Java.
 */
public final class AquariumCycle {

    /**
     * What the cycle starts from.
     *
     * @param stock        fish in the aquarium, fractions carried over
     * @param water        mB of water in the tank
     * @param dirt         dirt in the water
     * @param feed         feed items in the feed slot
     * @param unfedCycles  cycles in a row without enough feed so far
     * @param filterLeft   cleaning points left in the filter, 0 without one
     * @param poweredShare share of the cycle's ticks that had power, 0 to 1
     * @param keep         fish the catch leaves in the aquarium
     * @param autoCatch    whether fish above {@code keep} are caught
     * @param catchRoom    raw fish the catch slots still take
     */
    public record Input(double stock, long water, double dirt, int feed, int unfedCycles, int filterLeft,
                        double poweredShare, int keep, boolean autoCatch, int catchRoom) {
    }

    /**
     * What the cycle leaves.
     *
     * @param stock       fish afterwards
     * @param dirt        dirt afterwards
     * @param feedUsed    feed items eaten
     * @param unfedCycles cycles in a row without enough feed
     * @param filterUsed  cleaning points the filter spent
     * @param caught      fish moved into the catch slots as raw fish
     * @param died        fish that died, fractions included
     * @param quality     water quality after the filter, 0 to 1
     */
    public record Output(double stock, double dirt, int feedUsed, int unfedCycles, int filterUsed, int caught, double died,
                         double quality) {
    }

    private AquariumCycle() {
    }

    /** The fish {@code water} mB hold. */
    public static int capacity(long water, AquariumStats stats) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, water) / stats.waterPerFish());
    }

    /** Water quality: one minus the dirt over what the water holds at zero quality, 0 to 1; no water is 0. */
    public static double quality(double dirt, long water, AquariumStats stats) {
        double holds = water / 1000.0 * stats.dirtPerBucket();
        if (holds <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, 1 - dirt / holds));
    }

    /** The feed items {@code stock} fish eat in one cycle. */
    public static int feedNeeded(double stock, AquariumStats stats) {
        return (int) Math.ceil(Math.floor(stock) * stats.feedPerFish() - 1e-9);
    }

    public static Output run(Input in, AquariumStats stats) {
        int capacity = capacity(in.water(), stats);
        double share = Math.max(0, Math.min(1, in.poweredShare()));
        double stock = Math.max(0, in.stock());

        int need = feedNeeded(stock, stats);
        boolean fed = in.feed() >= need;
        int eaten = fed ? need : 0;
        int unfed = fed ? 0 : in.unfedCycles() + 1;

        double dirt = Math.max(0, in.dirt()) + Math.floor(stock) * stats.dirtPerFish() + eaten * stats.dirtPerFeed();

        int filterUsed = (int) Math.ceil(Math.min(dirt, stats.filterRate() * share) - 1e-9);
        filterUsed = Math.max(0, Math.min(filterUsed, in.filterLeft()));
        dirt = Math.max(0, dirt - filterUsed);

        double quality = quality(dirt, in.water(), stats);

        double before = stock;
        if (in.water() <= 0) {
            stock = 0;
        } else {
            if (quality < stats.dyingBelow()) {
                stock -= stock * stats.dirtyLoss();
            }
            if (unfed > stats.starveAfter()) {
                stock -= stock * stats.starveLoss();
            }
            stock = Math.min(stock, capacity);
        }
        double died = before - stock;

        if (fed && Math.floor(stock) >= 2 && quality >= stats.dyingBelow() && capacity > 0) {
            stock += stats.breedRate() * quality * share * stock * (1 - stock / capacity);
            stock = Math.min(stock, capacity);
        }

        int caught = 0;
        if (in.autoCatch()) {
            caught = (int) Math.max(0, Math.min(Math.floor(stock) - in.keep(), in.catchRoom()));
            stock -= caught;
        }
        return new Output(stock, dirt, eaten, unfed, filterUsed, caught, died, quality);
    }
}
