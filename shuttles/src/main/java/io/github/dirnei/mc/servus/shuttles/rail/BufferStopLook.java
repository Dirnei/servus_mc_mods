package io.github.dirnei.mc.servus.shuttles.rail;

import io.github.dirnei.mc.servus.shuttles.rail.RailLook.Box;
import io.github.dirnei.mc.servus.shuttles.rail.RailLook.Part;
import java.util.ArrayList;
import java.util.List;

/**
 * The look of the Buffer Stop in pixels, 16 to a block: a stub of track in
 * the rail's gauge running from the front edge under a striped beam, the beam
 * carried by two steel uprights with stepped braces on a steel sole, and a
 * buffer head on the beam in front of each rail head. Unrotated, the front
 * faces north (z 0), like the rail's end piece.
 */
public final class BufferStopLook {

    /** Where the sole starts along the track; the stub heads end here. */
    public static final double SOLE_FROM = 8;
    public static final double SOLE_TO = 15;
    public static final double BEAM_FROM = 7;
    public static final double BEAM_TO = 8;
    public static final double BEAM_BOTTOM = 6;
    public static final double BEAM_TOP = 9;
    public static final double UPRIGHT_TOP = 10;
    /** The stub's sleepers: the first two of the rail's four. */
    private static final int STUB_SLEEPERS = 2;
    /** How far a buffer head reaches past a rail head on each side. */
    private static final double BUFFER_OVERHANG = 0.5;
    private static final double BUFFER_BOTTOM = 6.25;
    private static final double BUFFER_TOP = 8.75;
    private static final double BUFFER_FROM = 6;
    /** The braces behind the uprights: from z, to z, top. */
    private static final double[][] BRACES = {{10, 12, 7}, {12, 14, 4}};

    private BufferStopLook() {
    }

    /** The unrotated boxes, front to the north. */
    public static List<Box> boxes() {
        List<Box> boxes = new ArrayList<>();
        for (int i = 0; i < STUB_SLEEPERS; i++) {
            int z = RailLook.SLEEPERS[i];
            boxes.add(new Box(Part.SLEEPER, RailLook.SLEEPER_FROM, 0, z, RailLook.SLEEPER_TO, RailLook.SLEEPER_TOP,
                    z + RailLook.SLEEPER_LENGTH));
        }
        for (int[] head : RailLook.HEADS) {
            boxes.add(new Box(Part.HEAD, head[0], RailLook.SLEEPER_TOP, 0, head[1], RailLook.HEAD_TOP, SOLE_FROM));
        }
        boxes.add(new Box(Part.FRAME, RailLook.HEADS[0][0] - 1, 0, SOLE_FROM, RailLook.HEADS[1][1] + 1, 1, SOLE_TO));
        for (int[] head : RailLook.HEADS) {
            boxes.add(new Box(Part.FRAME, head[0], 1, SOLE_FROM, head[1], UPRIGHT_TOP, BRACES[0][0]));
            for (double[] brace : BRACES) {
                boxes.add(new Box(Part.FRAME, head[0], 1, brace[0], head[1], brace[2], brace[1]));
            }
        }
        boxes.add(new Box(Part.DOCK, RailLook.SLEEPER_FROM, BEAM_BOTTOM, BEAM_FROM, RailLook.SLEEPER_TO, BEAM_TOP, BEAM_TO));
        for (int[] head : RailLook.HEADS) {
            boxes.add(new Box(Part.FRAME, head[0] - BUFFER_OVERHANG, BUFFER_BOTTOM, BUFFER_FROM,
                    head[1] + BUFFER_OVERHANG, BUFFER_TOP, BEAM_FROM));
        }
        return boxes;
    }

    /** The boxes turned clockwise by that many degrees, like a blockstate y rotation. */
    public static List<Box> boxes(int rotation) {
        return RailLook.rotated(boxes(), rotation);
    }
}
