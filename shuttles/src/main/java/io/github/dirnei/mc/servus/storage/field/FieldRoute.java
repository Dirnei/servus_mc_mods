package io.github.dirnei.mc.servus.storage.field;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** The order a harvester visits the rails of a field: always the nearest rail with work next. Pure Java. */
public final class FieldRoute {

    private FieldRoute() {
    }

    /**
     * The rail with work nearest to {@code from} by steps over the rails,
     * {@code from} itself included; empty when no reachable rail has work.
     */
    public static <T> Optional<T> next(T from, Function<T, Collection<T>> neighbours, Predicate<T> hasWork) {
        ArrayDeque<T> queue = new ArrayDeque<>();
        Set<T> seen = new HashSet<>();
        queue.add(from);
        seen.add(from);
        while (!queue.isEmpty()) {
            T rail = queue.poll();
            if (hasWork.test(rail)) {
                return Optional.of(rail);
            }
            for (T neighbour : neighbours.apply(rail)) {
                if (seen.add(neighbour)) {
                    queue.add(neighbour);
                }
            }
        }
        return Optional.empty();
    }

    /** Every rail with work in visiting order, each visited once, starting the search at {@code from}. */
    public static <T> List<T> order(T from, Function<T, Collection<T>> neighbours, Predicate<T> hasWork) {
        List<T> stops = new ArrayList<>();
        Set<T> done = new HashSet<>();
        T at = from;
        while (true) {
            Optional<T> next = next(at, neighbours, rail -> !done.contains(rail) && hasWork.test(rail));
            if (next.isEmpty()) {
                return stops;
            }
            at = next.get();
            done.add(at);
            stops.add(at);
        }
    }
}
