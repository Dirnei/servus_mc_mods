package io.github.dirnei.mc.servus.storage.field;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/** Collects the farm rails of one field: breadth first over farm rail neighbours. Pure, no Minecraft imports. */
public final class FieldGraph {

    private FieldGraph() {
    }

    /** The members found from the start, in visiting order, and whether the walk stopped because there were more than allowed. */
    public record Result<T>(Set<T> members, boolean tooLarge) {
    }

    /**
     * @param start      a farm rail of the field
     * @param neighbours the farm rails linked to a rail
     * @param max        the most rails a field may have
     */
    public static <T> Result<T> collect(T start, Function<T, Collection<T>> neighbours, int max) {
        Set<T> members = new LinkedHashSet<>();
        ArrayDeque<T> queue = new ArrayDeque<>();
        members.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            T current = queue.poll();
            for (T next : neighbours.apply(current)) {
                if (members.add(next)) {
                    if (members.size() > max) {
                        return new Result<>(members, true);
                    }
                    queue.add(next);
                }
            }
        }
        return new Result<>(members, false);
    }
}
