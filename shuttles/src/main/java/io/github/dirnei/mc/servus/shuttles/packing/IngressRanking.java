package io.github.dirnei.mc.servus.shuttles.packing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The order in which the warehouse offers items going into storage to its
 * Packing Stations: the cheapest shuttle trip from the station to the rack
 * the items go to first, stations no shuttle trip reaches last, the lowest
 * position on a tie. Pure Java.
 */
public final class IngressRanking {

    /**
     * A Packing Station that could take the items.
     *
     * @param id       what the caller knows the station by
     * @param cost     the trip cost to the rack, {@link PackingStationChoice#UNREACHABLE} without a way
     * @param position the station's packed position, for ties
     */
    public record Candidate<C>(C id, int cost, long position) {
    }

    private IngressRanking() {
    }

    public static <C> List<C> order(List<Candidate<C>> candidates) {
        List<Candidate<C>> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.<Candidate<C>>comparingInt(Candidate::cost).thenComparingLong(Candidate::position));
        List<C> ids = new ArrayList<>(sorted.size());
        for (Candidate<C> candidate : sorted) {
            ids.add(candidate.id());
        }
        return ids;
    }
}
