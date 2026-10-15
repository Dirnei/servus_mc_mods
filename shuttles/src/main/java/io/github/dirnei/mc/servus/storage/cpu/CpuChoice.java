package io.github.dirnei.mc.servus.storage.cpu;

import java.util.List;
import java.util.OptionalInt;

/**
 * Which CPU of a data line takes a new order: the first with a free slot,
 * else the one with the fewest open orders, the earliest on a tie. CPUs are
 * given in position order.
 */
public final class CpuChoice {

    /** A CPU's open orders and order slots. */
    public record Load(int open, int slots) {
    }

    private CpuChoice() {
    }

    /** The index of the chosen CPU, empty when there is none. */
    public static OptionalInt choose(List<Load> loads) {
        for (int i = 0; i < loads.size(); i++) {
            if (loads.get(i).open() < loads.get(i).slots()) {
                return OptionalInt.of(i);
            }
        }
        int best = -1;
        for (int i = 0; i < loads.size(); i++) {
            if (best < 0 || loads.get(i).open() < loads.get(best).open()) {
                best = i;
            }
        }
        return best < 0 ? OptionalInt.empty() : OptionalInt.of(best);
    }
}
