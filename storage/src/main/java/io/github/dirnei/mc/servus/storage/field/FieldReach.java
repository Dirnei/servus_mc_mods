package io.github.dirnei.mc.servus.storage.field;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Which plots the rails of a field can see, on the rails' level. Every rail
 * looks straight out, at 90 degrees, on each side without a farm rail next to
 * it; the first plot that is not open ends that line. Where a rail has two
 * such sides at a right angle, at the end of a run or on the outside of a
 * bend, it also sees the corner between them along an L: straight out on one
 * side, then 90 degrees towards the other, every plot of the L open. Nothing
 * is seen around a blocker. Pure, no Minecraft imports.
 */
public final class FieldReach {

    /** North, east, south, west; a direction's right angles are its neighbours in this order. */
    private static final int[][] STEPS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private FieldReach() {
    }

    /**
     * @param rails      the field's rails
     * @param candidates the plots within reach of a rail, the only ones that count
     * @param open       whether a candidate holds soil a harvester can work
     * @param radius     how far a rail looks out, the field radius
     */
    public static Set<FieldPos> connected(Collection<FieldPos> rails, Set<FieldPos> candidates, Predicate<FieldPos> open, int radius) {
        Set<FieldPos> railSet = new HashSet<>(rails);
        Set<FieldPos> reached = new LinkedHashSet<>();
        Predicate<FieldPos> seen = plot -> candidates.contains(plot) && open.test(plot);
        for (FieldPos rail : rails) {
            boolean[] free = new boolean[STEPS.length];
            for (int d = 0; d < STEPS.length; d++) {
                free[d] = !railSet.contains(step(rail, d, 1));
            }
            for (int d = 0; d < STEPS.length; d++) {
                if (!free[d]) {
                    continue;
                }
                for (int i = 1; i <= radius; i++) {
                    FieldPos out = step(rail, d, i);
                    if (!seen.test(out)) {
                        break;
                    }
                    reached.add(out);
                    for (int turn : new int[] {(d + 1) % 4, (d + 3) % 4}) {
                        if (!free[turn]) {
                            continue;
                        }
                        for (int j = 1; j <= radius; j++) {
                            FieldPos corner = step(out, turn, j);
                            if (!seen.test(corner)) {
                                break;
                            }
                            reached.add(corner);
                        }
                    }
                }
            }
        }
        return reached;
    }

    private static FieldPos step(FieldPos from, int direction, int distance) {
        return new FieldPos(from.x() + STEPS[direction][0] * distance, from.y(), from.z() + STEPS[direction][1] * distance);
    }
}
