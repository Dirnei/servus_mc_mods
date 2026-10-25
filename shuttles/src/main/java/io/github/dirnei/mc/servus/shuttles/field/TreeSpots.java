package io.github.dirnei.mc.servus.shuttles.field;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Where a Trees field plants, in world coordinates so the spots stay put when
 * rails are added. Single saplings go on every other plot in both directions;
 * 2x2 saplings go on squares of four plots with one plot between squares, so
 * no two squares touch. Pure Java.
 */
public final class TreeSpots {

    private static final int SQUARE_STRIDE = 3;

    private TreeSpots() {
    }

    /** Whether the plot is a spot for a single sapling: x and z both even. */
    public static boolean isSpot(int x, int z) {
        return Math.floorMod(x, 2) == 0 && Math.floorMod(z, 2) == 0;
    }

    /**
     * The square the plot belongs to, named by its smaller corner (x and z
     * divisible by 3); empty for a plot in the gap column or row.
     */
    public static Optional<FieldPos> square(FieldPos plot) {
        int offsetX = Math.floorMod(plot.x(), SQUARE_STRIDE);
        int offsetZ = Math.floorMod(plot.z(), SQUARE_STRIDE);
        if (offsetX == SQUARE_STRIDE - 1 || offsetZ == SQUARE_STRIDE - 1) {
            return Optional.empty();
        }
        return Optional.of(new FieldPos(plot.x() - offsetX, plot.y(), plot.z() - offsetZ));
    }

    /** The four plots of the square with that smaller corner. */
    public static List<FieldPos> members(FieldPos corner) {
        return List.of(corner, new FieldPos(corner.x() + 1, corner.y(), corner.z()),
                new FieldPos(corner.x(), corner.y(), corner.z() + 1), new FieldPos(corner.x() + 1, corner.y(), corner.z() + 1));
    }

    /** Whether all four plots of the square are plots of the field. */
    public static boolean complete(FieldPos corner, Predicate<FieldPos> fieldPlot) {
        for (FieldPos member : members(corner)) {
            if (!fieldPlot.test(member)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the plot is planted in a field of that layout: a single spot, or
     * a plot of a complete square.
     */
    public static boolean planted(FieldPos plot, boolean twoByTwo, Predicate<FieldPos> fieldPlot) {
        if (!twoByTwo) {
            return isSpot(plot.x(), plot.z());
        }
        return square(plot).map(corner -> complete(corner, fieldPlot)).orElse(false);
    }

    /** The plots a tree on that plot stands on: its square in a 2x2 field, else the plot alone. */
    public static List<FieldPos> base(FieldPos plot, boolean twoByTwo) {
        if (!twoByTwo) {
            return List.of(plot);
        }
        return square(plot).map(TreeSpots::members).orElse(List.of(plot));
    }
}
