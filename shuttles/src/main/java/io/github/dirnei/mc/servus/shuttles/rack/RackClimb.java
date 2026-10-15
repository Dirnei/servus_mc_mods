package io.github.dirnei.mc.servus.shuttles.rack;

import java.util.ArrayList;
import java.util.List;

/**
 * Rules for climbing the side posts of a rack like a ladder. Finds the
 * cells a player's box is pressed against, tells side faces from the front
 * and the back, and mirrors the vanilla ladder motion. Pure Java.
 */
public final class RackClimb {

    /** Vertical speed vanilla writes after the move while pushing into a ladder. */
    public static final double CLIMB_SPEED = 0.2;
    /** Fastest a hanging player slides down a ladder. */
    public static final double SLIDE_SPEED = 0.15;
    /** Vanilla air drag applied to vertical speed after gravity. */
    public static final double DRAG = 0.98;

    private RackClimb() {
    }

    /** An axis aligned box, such as a player's bounding box. */
    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    }

    /**
     * A cell directly beyond a face of the box that lies on a block boundary.
     * {@code dx, dz} is the unit step from the box into the cell.
     */
    public record Contact(int cellX, int cellY, int cellZ, int dx, int dz) {
    }

    /**
     * Cells beyond every vertical face of the box that sits on a block
     * boundary within {@code tolerance}. The cells span the box's extent on
     * the other horizontal axis and its full height.
     */
    public static List<Contact> contacts(Box box, double tolerance) {
        List<Contact> out = new ArrayList<>();
        int rowMin = (int) Math.floor(box.minY() + tolerance);
        int rowMax = (int) Math.floor(box.maxY() - tolerance);
        int colZMin = (int) Math.floor(box.minZ() + tolerance);
        int colZMax = (int) Math.floor(box.maxZ() - tolerance);
        int colXMin = (int) Math.floor(box.minX() + tolerance);
        int colXMax = (int) Math.floor(box.maxX() - tolerance);

        Integer west = boundary(box.minX(), tolerance);
        if (west != null) {
            addColumn(out, west - 1, null, -1, 0, colZMin, colZMax, rowMin, rowMax);
        }
        Integer east = boundary(box.maxX(), tolerance);
        if (east != null) {
            addColumn(out, east, null, 1, 0, colZMin, colZMax, rowMin, rowMax);
        }
        Integer north = boundary(box.minZ(), tolerance);
        if (north != null) {
            addColumn(out, null, north - 1, 0, -1, colXMin, colXMax, rowMin, rowMax);
        }
        Integer south = boundary(box.maxZ(), tolerance);
        if (south != null) {
            addColumn(out, null, south, 0, 1, colXMin, colXMax, rowMin, rowMax);
        }
        return out;
    }

    /** The block boundary {@code value} lies on, or null when it is not on one. */
    private static Integer boundary(double value, double tolerance) {
        long nearest = Math.round(value);
        return Math.abs(value - nearest) <= tolerance ? (int) nearest : null;
    }

    private static void addColumn(List<Contact> out, Integer fixedX, Integer fixedZ, int dx, int dz,
                                  int colMin, int colMax, int rowMin, int rowMax) {
        for (int col = colMin; col <= colMax; col++) {
            for (int row = rowMin; row <= rowMax; row++) {
                int x = fixedX != null ? fixedX : col;
                int z = fixedZ != null ? fixedZ : col;
                out.add(new Contact(x, row, z, dx, dz));
            }
        }
    }

    /**
     * Whether the face touched by stepping {@code dx, dz} into a rack is one
     * of its sides. The front and the back lie along the facing; the sides
     * are at a right angle to it.
     */
    public static boolean isSide(int facingDx, int facingDz, int dx, int dz) {
        return facingDx * dx + facingDz * dz == 0;
    }

    /**
     * Vertical speed to store after the tick's move, in the slot where
     * gravity and drag have already been applied for the next tick.
     *
     * @param current  the player's vertical speed after this tick
     * @param pushing  whether the player collided horizontally while touching a side
     * @param sneaking whether the player holds sneak
     * @param gravity  the player's gravity per tick
     */
    public static double verticalMotion(double current, boolean pushing, boolean sneaking, double gravity) {
        if (pushing) {
            return (CLIMB_SPEED - gravity) * DRAG;
        }
        double y = Math.max(current, -SLIDE_SPEED);
        if (y < 0.0 && sneaking) {
            return 0.0;
        }
        return y;
    }
}
