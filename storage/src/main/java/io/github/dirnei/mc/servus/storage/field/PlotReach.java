package io.github.dirnei.mc.servus.storage.field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which plots a harvester works from which rail: every plot from the rail
 * nearest to it, so a shuttle standing on a rail reaches at most the field
 * radius, the strip of the field. Pure, no Minecraft imports.
 */
public final class PlotReach {

    /** Radii with a cached offset order; larger ones are sorted on every call. */
    private static final int CACHED = 16;
    /** Offsets from a plot to a rail per radius, nearest first: Chebyshev distance, then Manhattan, then x, then z. */
    private static final List<List<int[]>> OFFSETS = new ArrayList<>(Collections.nCopies(CACHED + 1, null));

    private PlotReach() {
    }

    private static synchronized List<int[]> offsets(int radius) {
        if (radius > CACHED) {
            return sorted(radius);
        }
        List<int[]> cached = OFFSETS.get(radius);
        if (cached == null) {
            cached = sorted(radius);
            OFFSETS.set(radius, cached);
        }
        return cached;
    }

    private static List<int[]> sorted(int radius) {
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                offsets.add(new int[] {dx, dz});
            }
        }
        offsets.sort((a, b) -> {
            int byChebyshev = Integer.compare(Math.max(Math.abs(a[0]), Math.abs(a[1])), Math.max(Math.abs(b[0]), Math.abs(b[1])));
            if (byChebyshev != 0) {
                return byChebyshev;
            }
            int byManhattan = Integer.compare(Math.abs(a[0]) + Math.abs(a[1]), Math.abs(b[0]) + Math.abs(b[1]));
            if (byManhattan != 0) {
                return byManhattan;
            }
            return a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]);
        });
        return List.copyOf(offsets);
    }

    /**
     * The plots worked from each rail, nearest first. Plots with no rail
     * within radius blocks on their level are left out; rails without plots
     * map to an empty list.
     */
    public static Map<FieldPos, List<FieldPos>> assign(Collection<FieldPos> rails, Collection<FieldPos> plots, int radius) {
        List<int[]> offsets = offsets(Math.max(0, radius));
        Set<FieldPos> railSet = new HashSet<>(rails);
        Map<FieldPos, List<FieldPos>> byRail = new LinkedHashMap<>();
        Map<FieldPos, Integer> rank = new HashMap<>();
        for (FieldPos rail : rails) {
            byRail.put(rail, new ArrayList<>());
        }
        for (FieldPos plot : plots) {
            if (railSet.contains(plot)) {
                continue;
            }
            for (int i = 0; i < offsets.size(); i++) {
                int[] offset = offsets.get(i);
                FieldPos rail = new FieldPos(plot.x() + offset[0], plot.y(), plot.z() + offset[1]);
                if (railSet.contains(rail)) {
                    byRail.get(rail).add(plot);
                    rank.put(plot, i);
                    break;
                }
            }
        }
        for (List<FieldPos> list : byRail.values()) {
            list.sort((a, b) -> Integer.compare(rank.get(a), rank.get(b)));
        }
        return byRail;
    }
}
