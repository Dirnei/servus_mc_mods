package io.github.dirnei.mc.servus.storage.packing;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Picks the Packing Station of a site that receives a wireless transfer: the
 * one with the fewest open incoming transfers, the lowest position on a tie.
 * Pure, no Minecraft imports.
 */
public final class ReceiverChoice {

    private ReceiverChoice() {
    }

    /**
     * @param stations the receiving site's Packing Stations
     * @param incoming open incoming wireless transfers of a station
     * @param position a station's position as a comparable number (lower wins a tie)
     */
    public static <C> Optional<C> choose(List<C> stations, ToIntFunction<C> incoming, ToLongFunction<C> position) {
        C best = null;
        int bestCount = Integer.MAX_VALUE;
        long bestPos = Long.MAX_VALUE;
        for (C station : stations) {
            int count = Math.max(0, incoming.applyAsInt(station));
            long pos = position.applyAsLong(station);
            if (best == null || count < bestCount || count == bestCount && pos < bestPos) {
                best = station;
                bestCount = count;
                bestPos = pos;
            }
        }
        return Optional.ofNullable(best);
    }
}
