package io.github.dirnei.mc.servus.shuttles.rail;

/**
 * The centre piece of a rail from the sides that carry rail: straight along a
 * run or a single side, a curve on a corner, a crossing for three or four
 * sides. Rotation is clockwise seen from above in quarter turns of 90 degrees,
 * like a blockstate y rotation; the unrotated straight runs north to south and
 * the unrotated curve joins north and east.
 */
public record RailCenter(Shape shape, int rotation) {

    public enum Shape { STRAIGHT, CURVE, CROSS }

    public static RailCenter of(boolean north, boolean east, boolean south, boolean west) {
        int sides = (north ? 1 : 0) + (east ? 1 : 0) + (south ? 1 : 0) + (west ? 1 : 0);
        if (sides >= 3) {
            return new RailCenter(Shape.CROSS, 0);
        }
        if (sides == 2 && north != south) {
            return new RailCenter(Shape.CURVE, north ? (east ? 0 : 270) : (east ? 90 : 180));
        }
        return new RailCenter(Shape.STRAIGHT, east || west ? 90 : 0);
    }
}
