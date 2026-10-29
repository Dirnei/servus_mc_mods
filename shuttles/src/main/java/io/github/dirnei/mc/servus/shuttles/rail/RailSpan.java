package io.github.dirnei.mc.servus.shuttles.rail;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Whether a gap of free rails holds. Free rails that link to each other form a
 * gap; its span is the free rails plus the hung rails linked to them, its
 * holds are the standing and hung rails linked to them. A gap holds when the
 * span stays within the limit and it has at least two holds, or when it is a
 * single free rail with one hold (an overhang of one); otherwise its free
 * rails and the hung rails linked to them fall.
 */
public final class RailSpan {

    /** Standing on a floor, hung on an anchor, or free in the air. */
    public enum Kind { STANDING, HUNG, FREE }

    /** The verdict for one gap: the rails to drop are empty when it holds. */
    public record Verdict<N>(boolean holds, Set<N> drop) {
    }

    private RailSpan() {
    }

    /**
     * Checks the gap of the given rail. A rail that is not free always holds.
     * The walk stops once the span passes {@code maxSpan}, so a long free run
     * costs at most a few steps; the rails it did not reach fall when their
     * dropped neighbours make them check again.
     */
    public static <N> Verdict<N> check(N start, Function<N, Kind> kind, Function<N, ? extends Iterable<N>> links, int maxSpan) {
        if (kind.apply(start) != Kind.FREE) {
            return new Verdict<>(true, Set.of());
        }
        Set<N> free = new LinkedHashSet<>();
        Set<N> hung = new LinkedHashSet<>();
        Set<N> standing = new LinkedHashSet<>();
        Deque<N> queue = new ArrayDeque<>();
        free.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            for (N next : links.apply(queue.poll())) {
                switch (kind.apply(next)) {
                    case FREE -> {
                        if (free.add(next)) {
                            queue.add(next);
                        }
                    }
                    case HUNG -> hung.add(next);
                    case STANDING -> standing.add(next);
                }
                if (free.size() + hung.size() > maxSpan) {
                    return new Verdict<>(false, drop(free, hung));
                }
            }
        }
        int holds = hung.size() + standing.size();
        if (holds >= 2 || free.size() == 1 && holds == 1) {
            return new Verdict<>(true, Set.of());
        }
        return new Verdict<>(false, drop(free, hung));
    }

    private static <N> Set<N> drop(Set<N> free, Set<N> hung) {
        Set<N> drop = new LinkedHashSet<>(free);
        drop.addAll(hung);
        return drop;
    }
}
