package io.github.dirnei.mc.servus.storage.rail;

import java.util.ArrayList;
import java.util.List;

/**
 * The track look of a rail in pixels, 16 to a block: two raised heads at
 * 2..4 and 12..14 across the travel direction (y 1..2) on four sleepers per
 * block, each 2 long and 14 wide (y 0..1). Shared by the block models and the
 * elevator renderer. Unrotated pieces run north to south; the curve joins
 * north and east; the end and the dock face north. Rotation is clockwise seen
 * from above in quarter turns, like a blockstate y rotation.
 */
public final class RailLook {

    /** Across the travel direction: the two heads and the sleeper width. */
    public static final int[][] HEADS = {{2, 4}, {12, 14}};
    public static final int SLEEPER_FROM = 1;
    public static final int SLEEPER_TO = 15;
    /** Along the travel direction: where each of the four sleepers starts; each is 2 long. */
    public static final int[] SLEEPERS = {1, 5, 9, 13};
    public static final int SLEEPER_LENGTH = 2;
    public static final int SLEEPER_TOP = 1;
    public static final int HEAD_TOP = 2;
    public static final int DOCK_TOP = 3;

    public enum Piece { SINGLE, END, STRAIGHT, CURVE, CROSS }

    public enum Part { SLEEPER, HEAD, DOCK }

    /** A piece of a rail and its clockwise rotation in degrees. */
    public record Placed(Piece piece, int rotation) {
    }

    /** One box of a piece, in pixels. */
    public record Box(Part part, double x0, double y0, double z0, double x1, double y1, double z1) {

        /** This box turned clockwise around the block centre, seen from above. */
        public Box rotated(int rotation) {
            Box box = this;
            for (int i = 0; i < Math.floorMod(rotation, 360) / 90; i++) {
                box = new Box(box.part, 16 - box.z1, box.y0, box.x0, 16 - box.z0, box.y1, box.x1);
            }
            return box;
        }
    }

    private RailLook() {
    }

    /** The piece for the sides that carry rail (docks do not count). */
    public static Placed of(boolean north, boolean east, boolean south, boolean west) {
        int sides = (north ? 1 : 0) + (east ? 1 : 0) + (south ? 1 : 0) + (west ? 1 : 0);
        if (sides == 0) {
            return new Placed(Piece.SINGLE, 0);
        }
        if (sides == 1) {
            return new Placed(Piece.END, north ? 0 : east ? 90 : south ? 180 : 270);
        }
        RailCenter center = RailCenter.of(north, east, south, west);
        Piece piece = switch (center.shape()) {
            case STRAIGHT -> Piece.STRAIGHT;
            case CURVE -> Piece.CURVE;
            case CROSS -> Piece.CROSS;
        };
        return new Placed(piece, center.rotation());
    }

    /** The boxes of a placed piece, turned into the block. */
    public static List<Box> boxes(Placed placed) {
        return rotated(boxes(placed.piece()), placed.rotation());
    }

    public static List<Box> rotated(List<Box> boxes, int rotation) {
        List<Box> result = new ArrayList<>(boxes.size());
        for (Box box : boxes) {
            result.add(box.rotated(rotation));
        }
        return result;
    }

    /** The unrotated boxes of a piece. */
    public static List<Box> boxes(Piece piece) {
        return switch (piece) {
            case SINGLE -> track(4, 12);
            case END -> track(0, 12);
            case STRAIGHT -> track(0, 16);
            case CURVE -> curve();
            case CROSS -> cross();
        };
    }

    /** Straight track from z0 to z1: both heads and the sleepers that lie inside. */
    private static List<Box> track(int z0, int z1) {
        List<Box> boxes = new ArrayList<>();
        for (int start : SLEEPERS) {
            if (start >= z0 && start + SLEEPER_LENGTH <= z1) {
                boxes.add(sleeper(start));
            }
        }
        for (int[] head : HEADS) {
            boxes.add(new Box(Part.HEAD, head[0], SLEEPER_TOP, z0, head[1], HEAD_TOP, z1));
        }
        return boxes;
    }

    private static Box sleeper(int z) {
        return new Box(Part.SLEEPER, SLEEPER_FROM, 0, z, SLEEPER_TO, SLEEPER_TOP, z + SLEEPER_LENGTH);
    }

    /**
     * Both directions of heads crossing over a sleeper plate. The east west
     * heads stop at the north south heads, so no two head boxes overlap.
     */
    private static List<Box> cross() {
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(Part.SLEEPER, SLEEPER_FROM, 0, SLEEPER_FROM, SLEEPER_TO, SLEEPER_TOP, SLEEPER_TO));
        for (int[] head : HEADS) {
            boxes.add(new Box(Part.HEAD, head[0], SLEEPER_TOP, 0, head[1], HEAD_TOP, 16));
        }
        int[] gaps = {0, HEADS[0][0], HEADS[0][1], HEADS[1][0], HEADS[1][1], 16};
        for (int[] head : HEADS) {
            for (int i = 0; i < gaps.length; i += 2) {
                boxes.add(new Box(Part.HEAD, gaps[i], SLEEPER_TOP, head[0], gaps[i + 1], HEAD_TOP, head[1]));
            }
        }
        return boxes;
    }

    /** Whether the pixel at x, z of the curve is a head, measured from the north east corner. */
    public static boolean curveHead(int x, int z) {
        double distance = cornerDistance(x, z);
        for (int[] head : HEADS) {
            // A head at x across a north south run lies 16 - x from the east edge.
            if (distance >= 16 - head[1] && distance < 16 - head[0]) {
                return true;
            }
        }
        return false;
    }

    /** Whether the pixel at x, z of the curve is a sleeper: inside the sleeper ring at a sleeper's place along the arc. */
    public static boolean curveSleeper(int x, int z) {
        double distance = cornerDistance(x, z);
        if (distance < 16 - SLEEPER_TO || distance >= 16 - SLEEPER_FROM) {
            return false;
        }
        double along = Math.toDegrees(Math.atan2(z + 0.5, 16 - (x + 0.5))) / 90 * 16;
        double inStep = along - 4 * Math.floor(along / 4);
        return inStep >= SLEEPERS[0] && inStep < SLEEPERS[0] + SLEEPER_LENGTH;
    }

    private static double cornerDistance(int x, int z) {
        return Math.hypot(16 - (x + 0.5), z + 0.5);
    }

    /** The curve from north to east, rasterised per pixel; runs of pixels in a row merge into one box. */
    private static List<Box> curve() {
        List<Box> boxes = new ArrayList<>();
        for (int z = 0; z < 16; z++) {
            row(boxes, z, Part.SLEEPER, 0, SLEEPER_TOP);
            row(boxes, z, Part.HEAD, SLEEPER_TOP, HEAD_TOP);
        }
        return boxes;
    }

    private static void row(List<Box> boxes, int z, Part part, int y0, int y1) {
        int start = -1;
        for (int x = 0; x <= 16; x++) {
            boolean set = x < 16 && (part == Part.HEAD ? curveHead(x, z) : curveSleeper(x, z));
            if (set && start < 0) {
                start = x;
            } else if (!set && start >= 0) {
                boxes.add(new Box(part, start, y0, z, x, y1, z + 1));
                start = -1;
            }
        }
    }

    /** The striped buffer beam on the north edge of a dock side. */
    public static List<Box> dock() {
        return List.of(new Box(Part.DOCK, SLEEPER_FROM, 0, 0, SLEEPER_TO, DOCK_TOP, 1));
    }

    /** The track between the beam and the centre, for a rail that runs straight into its dock. */
    public static List<Box> dockInline() {
        List<Box> boxes = new ArrayList<>();
        boxes.add(sleeper(SLEEPERS[0]));
        for (int[] head : HEADS) {
            boxes.add(new Box(Part.HEAD, head[0], SLEEPER_TOP, 1, head[1], HEAD_TOP, 4));
        }
        return boxes;
    }
}
