package io.github.dirnei.mc.servus.lib.fluid;

import io.github.dirnei.mc.servus.lib.flow.WaterFilling;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Moves fluids from sources to sinks in one pass, with no rate of its own:
 * as much moves as the sources give and the sinks take. Fluids are opaque
 * keys compared with {@code equals}. Each source offers one key; each sink
 * takes one key per pass, the first offered key it accepts in source order,
 * so a sink holding a fluid only receives that fluid and an empty sink takes
 * the first fluid offered to it. Short supply of a key is shared fairly among
 * its sinks by water filling; what a sink cannot take in its fair share (for
 * example a sink that only fills in whole buckets) is offered to the sinks
 * in order afterwards. Pure Java.
 */
public final class FluidRouting {

    private FluidRouting() {
    }

    /** What a source can give right now: a fluid key and an amount. */
    public record Offer<K>(K key, int amount) {
    }

    /** Something fluid can be drained from. */
    public interface Source<K> {

        /** What this source would give now, without changing anything; null or a non positive amount for nothing. */
        Offer<K> offer();

        /**
         * Drains for real.
         *
         * @return the amount of {@code key} drained, at most {@code amount}
         */
        int drain(K key, int amount);
    }

    /** Something fluid can be filled into. */
    @FunctionalInterface
    public interface Sink<K> {

        /**
         * @param simulate when true, nothing changes
         * @return the amount of {@code key} accepted (or that would be accepted)
         */
        int fill(K key, int amount, boolean simulate);
    }

    /** Per key bookkeeping of one pass. */
    private static final class Supply<K> {
        final K key;
        final List<Integer> sources = new ArrayList<>();
        final List<Integer> sinks = new ArrayList<>();
        final List<Integer> demands = new ArrayList<>();
        long total;

        Supply(K key) {
            this.key = key;
        }
    }

    /**
     * @param sources fluid providers, drained in list order
     * @param sinks   fluid receivers, served fairly per key
     * @return the total amount moved
     */
    public static <K> int route(List<? extends Source<K>> sources, List<? extends Sink<K>> sinks) {
        if (sources.isEmpty() || sinks.isEmpty()) {
            return 0;
        }
        int[] remaining = new int[sources.size()];
        Map<K, Supply<K>> supplies = new LinkedHashMap<>();
        for (int i = 0; i < sources.size(); i++) {
            Offer<K> offer = sources.get(i).offer();
            if (offer == null || offer.key() == null || offer.amount() <= 0) {
                continue;
            }
            remaining[i] = offer.amount();
            Supply<K> supply = supplies.computeIfAbsent(offer.key(), Supply::new);
            supply.sources.add(i);
            supply.total += offer.amount();
        }
        if (supplies.isEmpty()) {
            return 0;
        }
        for (int j = 0; j < sinks.size(); j++) {
            Sink<K> sink = sinks.get(j);
            for (Supply<K> supply : supplies.values()) {
                int demand = sink.fill(supply.key, clamp(supply.total), true);
                if (demand > 0) {
                    supply.sinks.add(j);
                    supply.demands.add(demand);
                    break;
                }
            }
        }
        int moved = 0;
        for (Supply<K> supply : supplies.values()) {
            if (supply.sinks.isEmpty()) {
                continue;
            }
            int[] demand = new int[supply.sinks.size()];
            long totalDemand = 0;
            for (int k = 0; k < demand.length; k++) {
                demand[k] = supply.demands.get(k);
                totalDemand += demand[k];
            }
            int[] allocation = WaterFilling.allocate(demand, clamp(Math.min(totalDemand, supply.total)));
            int[] filled = new int[demand.length];
            for (int k = 0; k < demand.length; k++) {
                filled[k] = deliver(supply, sources, remaining, sinks.get(supply.sinks.get(k)), allocation[k]);
                moved += filled[k];
            }
            // Whatever a sink could not take as its share goes to the sinks in order.
            for (int k = 0; k < demand.length; k++) {
                int wanted = demand[k] - filled[k];
                if (wanted > 0) {
                    moved += deliver(supply, sources, remaining, sinks.get(supply.sinks.get(k)), wanted);
                }
            }
        }
        return moved;
    }

    /** Fills up to {@code wanted} of the key into one sink, draining the key's sources in order. */
    private static <K> int deliver(Supply<K> supply, List<? extends Source<K>> sources, int[] remaining, Sink<K> sink, int wanted) {
        int filled = 0;
        for (int index : supply.sources) {
            if (wanted <= 0) {
                break;
            }
            int take = Math.min(wanted, remaining[index]);
            if (take <= 0) {
                continue;
            }
            int accepted = sink.fill(supply.key, take, true);
            if (accepted <= 0) {
                break;
            }
            int drained = sources.get(index).drain(supply.key, Math.min(accepted, take));
            if (drained <= 0) {
                remaining[index] = 0;
                continue;
            }
            remaining[index] -= drained;
            wanted -= drained;
            filled += sink.fill(supply.key, drained, false);
        }
        return filled;
    }

    private static int clamp(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }
}
