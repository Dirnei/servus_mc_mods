package io.github.dirnei.mc.servus.storage.access;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Picks the Access Point a shipment leaves through: the one with the shortest
 * shuttle trip, the first on a tie. A trip of {@link #UNREACHABLE} only wins
 * when no candidate has a trip. Pure, no Minecraft imports.
 */
public final class AccessPointChoice {

    /** The trip of a candidate no shuttle path reaches. */
    public static final int UNREACHABLE = Integer.MAX_VALUE;

    private AccessPointChoice() {
    }

    public static <C> Optional<C> choose(List<C> candidates, ToIntFunction<C> trip) {
        C best = null;
        int bestTrip = UNREACHABLE;
        for (C candidate : candidates) {
            int length = trip.applyAsInt(candidate);
            if (best == null || length < bestTrip) {
                best = candidate;
                bestTrip = length;
            }
        }
        return Optional.ofNullable(best);
    }
}
