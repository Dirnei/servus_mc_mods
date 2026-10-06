package io.github.dirnei.mc.servus.storage.shuttle;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a terminal request into pick jobs: instant ones from stock no
 * shuttle can reach, shuttle ones from stock a shuttle can reach, never an
 * instant pick from a rack. Pure, no Minecraft imports.
 */
public final class RequestPlan {

    private RequestPlan() {
    }

    /**
     * Stock of the requested item in one source.
     *
     * @param source    the source
     * @param count     how many it holds
     * @param rack      whether the source is a rack, which only shuttles serve
     * @param reachable whether a shuttle can drive to a rail beside it
     */
    public record Stock<S>(S source, int count, boolean rack, boolean reachable) {
    }

    /** One pick: up to a stack from one source, instant or by shuttle. */
    public record Job<S>(S source, int count, boolean byShuttle) {
    }

    /**
     * @param jobs         picks in the order to run them
     * @param unservable   what the stock could not cover
     * @param needsShuttle whether rack stock was skipped for lack of a shuttle
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
     * @param hasShuttle whether the warehouse has a shuttle that can serve this terminal
     * @param stock      where the item is, in index order
     */
    public static <S> Plan<S> plan(int amount, int maxStack, boolean hasShuttle, List<Stock<S>> stock) {
        List<Job<S>> jobs = new ArrayList<>();
        int wanted = Math.max(0, amount);
        boolean needsShuttle = false;
        // Instant picks first: they arrive at once.
        for (Stock<S> entry : stock) {
            if (wanted <= 0) {
                break;
            }
            if (entry.rack() || entry.reachable() && hasShuttle) {
                continue;
            }
            wanted -= add(jobs, entry, wanted, maxStack, false);
        }
        for (Stock<S> entry : stock) {
            if (wanted <= 0) {
                break;
            }
            if (entry.rack() && (!entry.reachable() || !hasShuttle)) {
                needsShuttle = needsShuttle || entry.count() > 0;
                continue;
            }
            if (!entry.reachable() || !hasShuttle) {
                continue;
            }
            wanted -= add(jobs, entry, wanted, maxStack, true);
        }
        return new Plan<>(List.copyOf(jobs), wanted, needsShuttle && wanted > 0);
    }

    private static <S> int add(List<Job<S>> jobs, Stock<S> entry, int wanted, int maxStack, boolean byShuttle) {
        int available = Math.min(entry.count(), wanted);
        int taken = 0;
        while (available > 0) {
            int count = Math.min(available, Math.max(1, maxStack));
            jobs.add(new Job<>(entry.source(), count, byShuttle));
            available -= count;
            taken += count;
        }
        return taken;
    }
}
