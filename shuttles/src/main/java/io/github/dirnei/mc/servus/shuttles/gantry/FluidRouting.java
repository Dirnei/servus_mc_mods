package io.github.dirnei.mc.servus.shuttles.gantry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Where a tanker brings a fluid: the Storage gantry tanks with room for it,
 * in the order they are tried. Tanks locked to the fluid come first, then
 * tanks on Any holding it, then empty tanks on Any; within one rank the
 * shorter way wins. Fluid that runs on the way into a tank counts as if it
 * were in it already, so an empty tank a run brings lava to holds lava. Pure
 * Java; {@code K} is a fluid key compared with {@code equals}, {@code T} names a tank.
 */
public final class FluidRouting {

    /**
     * One Storage tank the fluid could go to.
     *
     * @param id          names the tank
     * @param tank        its lock, content, amount and capacity
     * @param incoming    what runs on their way will put into it
     * @param incomingKey the fluid those runs bring, null when none is on its way
     * @param distance    how far it is from the source, in rail steps
     */
    public record Candidate<K, T>(T id, GantryTanks.Tank<K> tank, int incoming, K incomingKey, int distance) {

        /** The tank as it will be once every run on its way arrived. */
        GantryTanks.Tank<K> planned() {
            K content = tank.isEmpty() ? incomingKey : tank.content();
            int amount = (tank.isEmpty() ? 0 : tank.amount()) + Math.max(0, incoming);
            return new GantryTanks.Tank<>(tank.lock(), amount > 0 ? content : null, amount, tank.capacity());
        }
    }

    /** A tank with room for the fluid and how much room it has. */
    public record Target<T>(T id, int room) {
    }

    private FluidRouting() {
    }

    /** The tanks that take {@code key} with room left, best first. */
    public static <K, T> List<Target<T>> targets(List<Candidate<K, T>> candidates, K key) {
        record Ranked<K, T>(Candidate<K, T> candidate, int rank, int room) {
        }
        List<Ranked<K, T>> ranked = new ArrayList<>();
        for (Candidate<K, T> candidate : candidates) {
            GantryTanks.Tank<K> planned = candidate.planned();
            if (planned.room() > 0 && planned.accepts(key)) {
                ranked.add(new Ranked<>(candidate, rank(planned), planned.room()));
            }
        }
        ranked.sort(Comparator.<Ranked<K, T>>comparingInt(Ranked::rank).thenComparingInt(entry -> entry.candidate().distance()));
        List<Target<T>> targets = new ArrayList<>(ranked.size());
        for (Ranked<K, T> entry : ranked) {
            targets.add(new Target<>(entry.candidate().id(), entry.room()));
        }
        return targets;
    }

    private static int rank(GantryTanks.Tank<?> tank) {
        if (tank.lock() != null) {
            return 0;
        }
        return tank.isEmpty() ? 2 : 1;
    }
}
