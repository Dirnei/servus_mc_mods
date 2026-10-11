package io.github.dirnei.mc.servus.storage.field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The ground a field covers: the 9 by 9 square around each farm rail. Plots
 * are the positions at rail level, the hydration boxes cover the farmland one
 * level below. Pure, no Minecraft imports.
 */
public final class FieldStrip {

    /** Blocks the field reaches to each side of a rail. */
    public static final int RADIUS = 4;

    private FieldStrip() {
    }

    /** A box with inclusive minimum and exclusive maximum corners. */
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        public boolean contains(int x, int y, int z) {
            return x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ;
        }
    }

    /** Every position at the rails' level within {@link #RADIUS} of a rail in x and z, except the rails. */
    public static Set<FieldPos> plots(Collection<FieldPos> rails) {
        Set<FieldPos> railSet = new HashSet<>(rails);
        Set<FieldPos> plots = new LinkedHashSet<>();
        for (FieldPos rail : rails) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    FieldPos plot = new FieldPos(rail.x() + dx, rail.y(), rail.z() + dz);
                    if (!railSet.contains(plot)) {
                        plots.add(plot);
                    }
                }
            }
        }
        return plots;
    }

    /**
     * Hydration boxes: one per unbroken run of rails along x in the same row,
     * reaching {@link #RADIUS} blocks out and covering the layer one below the rails.
     */
    public static List<Box> boxes(Collection<FieldPos> rails) {
        Map<Long, List<Integer>> rows = new TreeMap<>();
        for (FieldPos rail : rails) {
            long row = ((long) rail.y() << 32) | (rail.z() & 0xFFFFFFFFL);
            rows.computeIfAbsent(row, key -> new ArrayList<>()).add(rail.x());
        }
        List<Box> boxes = new ArrayList<>();
        for (Map.Entry<Long, List<Integer>> entry : rows.entrySet()) {
            int y = (int) (entry.getKey() >> 32);
            int z = (int) (long) entry.getKey();
            List<Integer> xs = entry.getValue();
            xs.sort(Integer::compare);
            int start = xs.get(0);
            int previous = start;
            for (int i = 1; i <= xs.size(); i++) {
                if (i < xs.size() && xs.get(i) == previous + 1) {
                    previous = xs.get(i);
                    continue;
                }
                boxes.add(new Box(start - RADIUS, y - 1, z - RADIUS, previous + RADIUS + 1, y, z + RADIUS + 1));
                if (i < xs.size()) {
                    start = xs.get(i);
                    previous = start;
                }
            }
        }
        return boxes;
    }
}
