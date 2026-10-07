package io.github.dirnei.mc.servus.manufacturing.machine.sink;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry of the 2x2x2 Sink, without Minecraft types. A part number has
 * three bits seen from the front: right (1), up (2) and back (4). Part 0, the
 * front lower left block, is the controller. The facing is the front's
 * horizontal unit vector {@code (fx, fz)}; right is the right hand of a player
 * looking at the front (facing north, right is west).
 */
public final class SinkLayout {

    public static final int PARTS = 8;
    public static final int RIGHT = 1;
    public static final int UP = 2;
    public static final int BACK = 4;
    public static final int CONTROLLER = 0;

    private static final List<Integer> TOP_PARTS = List.of(2, 3, 6, 7);
    private static final List<Integer> FRONT_PARTS = List.of(0, 1, 2, 3);

    private SinkLayout() {
    }

    /** A block offset or position. */
    public record Vec(int x, int y, int z) {

        public Vec plus(Vec other) {
            return new Vec(x + other.x, y + other.y, z + other.z);
        }

        public Vec minus(Vec other) {
            return new Vec(x - other.x, y - other.y, z - other.z);
        }
    }

    /** The offset from the controller to {@code part} for a front facing {@code (fx, fz)}. */
    public static Vec offset(int part, int fx, int fz) {
        checkPart(part);
        checkFacing(fx, fz);
        int x = 0;
        int y = 0;
        int z = 0;
        if ((part & RIGHT) != 0) {
            x += fz;
            z -= fx;
        }
        if ((part & UP) != 0) {
            y += 1;
        }
        if ((part & BACK) != 0) {
            x -= fx;
            z -= fz;
        }
        return new Vec(x, y, z);
    }

    /** The controller position of the Sink whose {@code part} sits at {@code (x, y, z)}. */
    public static Vec controllerOf(int x, int y, int z, int fx, int fz, int part) {
        return new Vec(x, y, z).minus(offset(part, fx, fz));
    }

    /**
     * The controller positions of the four cubes whose front contains the block at
     * {@code (x, y, z)}, clicked on its {@code (fx, fz)} face; the cube lies behind
     * that face. Ordered by the clicked block's part, 0 to 3.
     */
    public static List<Vec> candidates(int x, int y, int z, int fx, int fz) {
        List<Vec> result = new ArrayList<>(FRONT_PARTS.size());
        for (int part : FRONT_PARTS) {
            result.add(controllerOf(x, y, z, fx, fz, part));
        }
        return result;
    }

    /** The four parts on top of the cube, where chimney columns stand. */
    public static List<Integer> topParts() {
        return TOP_PARTS;
    }

    /** The four parts whose front faces show the fan. */
    public static List<Integer> frontParts() {
        return FRONT_PARTS;
    }

    public static boolean isFront(int part) {
        checkPart(part);
        return (part & BACK) == 0;
    }

    public static boolean isTop(int part) {
        checkPart(part);
        return (part & UP) != 0;
    }

    private static void checkPart(int part) {
        if (part < 0 || part >= PARTS) {
            throw new IllegalArgumentException("No Sink part " + part);
        }
    }

    private static void checkFacing(int fx, int fz) {
        if (Math.abs(fx) + Math.abs(fz) != 1) {
            throw new IllegalArgumentException("Not a horizontal facing: " + fx + ", " + fz);
        }
    }
}
