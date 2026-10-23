package io.github.dirnei.mc.servus.shuttles.shuttle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Splits a terminal request into shuttle pick jobs from the stock a shuttle
 * can reach. Stock no shuttle reaches is never picked. Pure Java.
 */
public final class RequestPlan {

    private RequestPlan() {
    }

    /**
     * Stock of the requested item in one source.
     *
     * @param source    the source
     * @param count     how many it holds
     * @param reachable whether a shuttle can drive to a rail in front of it
     * @param cost      the travel cost between it and the destination; cheaper sources are picked first
     */
    public record Stock<S>(S source, int count, boolean reachable, double cost) {

        /** Stock without a cost, picked in the order given. */
        public Stock(S source, int count, boolean reachable) {
            this(source, count, reachable, 0);
        }
    }

    /** One shuttle pick: up to a stack from one source. */
    public record Job<S>(S source, int count) {
    }

    /**
     * @param jobs         picks in the order to run them
     * @param unservable   what the planned picks do not cover
     * @param needsShuttle whether stock was skipped because no shuttle reaches it
     */
    public record Plan<S>(List<Job<S>> jobs, int unservable, boolean needsShuttle) {
        public int planned() {
            int sum = 0;
            for (Job<S> job : jobs) {
                sum += job.count();
            }
            return sum;
        }
    }

    /**
     * @param amount     how many the player asked for
     * @param maxStack   the item's stack size, the most one job carries
     * @param hasShuttle whether the warehouse has a shuttle that can serve the destination
     * @param stock      where the item is, in index order; the cheapest is picked first, index order on equal costs
     */
    public static <S> Plan<S> plan(int amount, int maxStack, boolean hasShuttle, List<Stock<S>> stock) {
        List<Stock<S>> sorted = new ArrayList<>(stock);
        sorted.sort(Comparator.comparingDouble(Stock::cost));
        List<Job<S>> jobs = new ArrayList<>();
        int wanted = Math.max(0, amount);
        boolean needsShuttle = false;
        for (Stock<S> entry : sorted) {
            if (wanted <= 0) {
                break;
            }
            if (!entry.reachable() || !hasShuttle) {
                needsShuttle = needsShuttle || entry.count() > 0;
                continue;
            }
            wanted -= add(jobs, entry, wanted, maxStack);
        }
        return new Plan<>(List.copyOf(jobs), wanted, needsShuttle && wanted > 0);
    }

    private static <S> int add(List<Job<S>> jobs, Stock<S> entry, int wanted, int maxStack) {
        int available = Math.min(entry.count(), wanted);
        int taken = 0;
        while (available > 0) {
            int count = Math.min(available, Math.max(1, maxStack));
            jobs.add(new Job<>(entry.source(), count));
            available -= count;
            taken += count;
        }
        return taken;
    }
}
