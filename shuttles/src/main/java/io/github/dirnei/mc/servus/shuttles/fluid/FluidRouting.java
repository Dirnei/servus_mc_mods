package io.github.dirnei.mc.servus.shuttles.fluid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Where a tanker brings a fluid: the fluid stores (Fluid Rack shelves)
 * with room for it, in the order they are tried. Stores that prefer the
 * fluid come first (a shelf zoned for it), then stores holding it, then empty stores that accept it; within one
 * rank the shorter way wins. Pure Java; {@code T} names a store.
 */
public final class FluidRouting {

    /**
     * One fluid store as routing sees it for one fluid.
     *
     * @param id        names the store
     * @param preferred whether it prefers the fluid: locked or zoned to it
     * @param holds     whether it holds the fluid, or runs on their way bring it
     * @param accepts   whether it takes the fluid at all: its lock or zone allows it and it holds nothing else
     * @param room      what it still takes once every run on its way arrived
     * @param cost      how far it is from the source, in rail steps
     */
    public record Candidate<T>(T id, boolean preferred, boolean holds, boolean accepts, int room, int cost) {

        /**
         * The candidate for {@code key}, with fluid that runs on the way into
         * the store counted as if it were in it already, so an empty store a
         * run brings lava to holds lava. {@code K} is a fluid key compared
         * with {@code equals}; a null content or incoming key means none.
         *
         * @param allowed     whether its lock or zone lets the key in
         * @param content     the fluid held, null when empty
         * @param amount      how much it holds
         * @param capacity    how much it may hold
         * @param incoming    what runs on their way will put into it
         * @param incomingKey the fluid those runs bring
         */
        public static <K, T> Candidate<T> of(T id, K key, boolean preferred, boolean allowed, K content, int amount, int capacity,
                                             int incoming, K incomingKey, int cost) {
            int planned = (content == null ? 0 : Math.max(0, amount)) + Math.max(0, incoming);
            K plannedContent = content != null && amount > 0 ? content : incomingKey;
            K held = planned > 0 ? plannedContent : null;
            boolean holds = held != null && held.equals(key);
            return new Candidate<>(id, preferred, holds, allowed && (held == null || holds), Math.max(0, capacity - planned), cost);
        }
    }

    /** A store with room for the fluid and how much room it has. */
    public record Target<T>(T id, int room) {
    }

    private FluidRouting() {
    }

    /** The candidates that accept the fluid with room left, best first. */
    public static <T> List<Target<T>> targets(List<Candidate<T>> candidates) {
        List<Candidate<T>> ranked = new ArrayList<>();
        for (Candidate<T> candidate : candidates) {
            if (candidate.room() > 0 && candidate.accepts()) {
                ranked.add(candidate);
            }
        }
        ranked.sort(Comparator.<Candidate<T>>comparingInt(FluidRouting::rank).thenComparingInt(Candidate::cost));
        List<Target<T>> targets = new ArrayList<>(ranked.size());
        for (Candidate<T> candidate : ranked) {
            targets.add(new Target<>(candidate.id(), candidate.room()));
        }
        return targets;
    }

    private static int rank(Candidate<?> candidate) {
        if (candidate.preferred()) {
            return 0;
        }
        return candidate.holds() ? 1 : 2;
    }
}
