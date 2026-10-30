package io.github.dirnei.mc.servus.shuttles.zone;

import java.util.List;

/**
 * The address of a rack shelf relative to its warehouse's controller, such as
 * {@code N12.E5+2}: north or south distance, east or west distance, then the
 * signed height. North is negative Z and east positive X; zero distances are
 * written {@code N0} and {@code E0}, zero height {@code +0}. A shelf takes the
 * code of its code bay, the bay closest to the controller. Pure Java.
 */
public final class LocationCode {

    /** A bay's offset from the controller in blocks. */
    public record Offset(int dx, int dy, int dz) {

        long distanceSquared() {
            return (long) dx * dx + (long) dy * dy + (long) dz * dz;
        }
    }

    private LocationCode() {
    }

    /** The code of a bay {@code dx}, {@code dy}, {@code dz} blocks from the controller. */
    public static String of(int dx, int dy, int dz) {
        String northSouth = dz > 0 ? "S" + dz : "N" + -dz;
        String eastWest = dx < 0 ? "W" + -dx : "E" + dx;
        String height = dy < 0 ? "-" + -dy : "+" + dy;
        return northSouth + "." + eastWest + height;
    }

    /**
     * The index of a shelf's code bay: the bay with the smallest straight-line
     * distance to the controller, the first one on a tie. {@code bays} are the
     * shelf's bays from left to right seen from the front, so a tie goes to the
     * leftmost bay.
     *
     * @throws IllegalArgumentException when the shelf has no bay
     */
    public static int codeBay(List<Offset> bays) {
        if (bays.isEmpty()) {
            throw new IllegalArgumentException("a shelf has at least one bay");
        }
        int best = 0;
        for (int i = 1; i < bays.size(); i++) {
            if (bays.get(i).distanceSquared() < bays.get(best).distanceSquared()) {
                best = i;
            }
        }
        return best;
    }

    /** The code of a shelf: the code of its {@link #codeBay code bay}. */
    public static String ofShelf(List<Offset> bays) {
        Offset bay = bays.get(codeBay(bays));
        return of(bay.dx(), bay.dy(), bay.dz());
    }
}
