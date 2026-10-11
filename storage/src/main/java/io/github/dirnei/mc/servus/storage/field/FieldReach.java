package io.github.dirnei.mc.servus.storage.field;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Which plots the rails of a field can see: the open plots joined to a rail
 * through a chain of open plots, stepping north, east, south and west on the
 * rails' level. A path, a wall or water between rail and soil ends the field
 * there. Pure, no Minecraft imports.
 */
public final class FieldReach {

    private static final int[][] STEPS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private FieldReach() {
    }

    /**
     * @param rails      the field's rails
     * @param candidates the plots within reach of a rail, the only ones that count
     * @param open       whether a candidate holds soil a harvester can work
     */
    public static Set<FieldPos> connected(Collection<FieldPos> rails, Set<FieldPos> candidates, Predicate<FieldPos> open) {
        Set<FieldPos> reached = new LinkedHashSet<>();
        Set<FieldPos> seen = new HashSet<>();
        ArrayDeque<FieldPos> queue = new ArrayDeque<>(rails);
        while (!queue.isEmpty()) {
            FieldPos from = queue.poll();
            for (int[] step : STEPS) {
                FieldPos next = new FieldPos(from.x() + step[0], from.y(), from.z() + step[1]);
                if (candidates.contains(next) && seen.add(next) && open.test(next)) {
                    reached.add(next);
                    queue.add(next);
                }
            }
        }
        return reached;
    }
}
