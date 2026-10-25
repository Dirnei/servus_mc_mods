package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.shuttles.shuttle.module.ModuleSpec;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleLook;
import java.util.ArrayList;
import java.util.List;

/**
 * Where a shuttle's lamps and number plates sit, in the shuttle model's
 * pixels (+z the front, the body spans 2 to 14). Every part stands
 * {@link #GAP} off the face it is mounted on, so no face shares a plane with
 * the body. Headlights sit on the front, tail lights on the back, both above
 * the Harvester reel and beside the Handling mast and the spoiler struts; a
 * Spreader's hopper hides the back, so its tail lights stand on the hopper
 * rim. A Lumber shuttle's walls fold away, so its lamps sit on the chassis.
 * The plates sit on both long sides. Pure Java.
 */
public final class SignalLayout {

    /** The gap between a lamp or plate and the face it is mounted on. */
    public static final double GAP = 0.05;
    /** How far a lamp or plate stands out. */
    public static final double DEPTH = 0.5;
    /** The body's front, back and long side faces. */
    public static final double FRONT = 14;
    public static final double BACK = 2;
    public static final double LEFT = 2;
    public static final double RIGHT = 14;
    /** The top of a Spreader hopper's rim, where its tail lights stand. */
    public static final double SPREADER_RIM_TOP = 12;
    /** The rim's back half reaches this far; the lamps stand inside the shuttle's block. */
    public static final double SPREADER_LAMP_BACK = -1.95;
    /** Text pixels per model pixel for a short number, and the longest plate along the body. */
    public static final double TEXT_SCALE = 0.3;
    public static final double MAX_PLATE = 10;
    /** The plate's margin around the number along the body, both ends together. */
    public static final double PLATE_MARGIN = 1;
    /** The height of the font's digits in text pixels. */
    public static final double GLYPH_HEIGHT = 7;

    private static final double[] HEAD_X = {3.25, 11.25};
    private static final double HEAD_WIDTH = 1.5;
    private static final double[] TAIL_X = {4.05, 10.55};
    private static final double TAIL_WIDTH = 1.4;
    private static final double LAMP_HEIGHT = 1;
    private static final double LAMP_Y = 7.5;
    private static final double CHASSIS_LAMP_Y = 2.4;
    private static final double PLATE_Y = 5;
    private static final double LUMBER_PLATE_Y = 6.5;
    private static final double PLATE_HEIGHT = 3;
    private static final double PLATE_CENTRE_Z = 8;

    /** One box from its low to its high corner. */
    public record Box(double x1, double y1, double z1, double x2, double y2, double z2) {
    }

    /** The headlights on the front and the tail lights on the back. */
    public record Lamps(List<Box> head, List<Box> tail) {
    }

    /**
     * Both number plates and the text on them: the left plate faces -x, the
     * right one +x. The text is {@code scale} model pixels per text pixel,
     * centred on the plate, its digits' top at {@code textTop}.
     */
    public record Plates(Box left, Box right, double scale, double textTop) {
    }

    private SignalLayout() {
    }

    public static Lamps lamps(ShuttleLook.Key key) {
        boolean lumber = ShuttleLook.lumber(key.tool());
        double y = lumber ? CHASSIS_LAMP_Y : LAMP_Y;
        List<Box> head = new ArrayList<>();
        for (double x : HEAD_X) {
            head.add(new Box(x, y, FRONT + GAP, x + HEAD_WIDTH, y + LAMP_HEIGHT, FRONT + GAP + DEPTH));
        }
        List<Box> tail = new ArrayList<>();
        for (double x : TAIL_X) {
            if (spreader(key)) {
                double bottom = SPREADER_RIM_TOP + GAP;
                tail.add(new Box(x, bottom, SPREADER_LAMP_BACK, x + TAIL_WIDTH, bottom + LAMP_HEIGHT, SPREADER_LAMP_BACK + DEPTH));
            } else {
                tail.add(new Box(x, y, BACK - GAP - DEPTH, x + TAIL_WIDTH, y + LAMP_HEIGHT, BACK - GAP));
            }
        }
        return new Lamps(head, tail);
    }

    /**
     * The plates for a number whose text is {@code textWidth} text pixels
     * wide: as long as the number and its margin, the text shrunk so the plate
     * stays within {@link #MAX_PLATE}.
     */
    public static Plates plates(ShuttleLook.Key key, int textWidth) {
        double scale = Math.min(TEXT_SCALE, (MAX_PLATE - PLATE_MARGIN) / Math.max(1, textWidth));
        double length = textWidth * scale + PLATE_MARGIN;
        double y = ShuttleLook.lumber(key.tool()) ? LUMBER_PLATE_Y : PLATE_Y;
        double z1 = PLATE_CENTRE_Z - length / 2;
        double z2 = PLATE_CENTRE_Z + length / 2;
        Box left = new Box(LEFT - GAP - DEPTH, y, z1, LEFT - GAP, y + PLATE_HEIGHT, z2);
        Box right = new Box(RIGHT + GAP, y, z1, RIGHT + GAP + DEPTH, y + PLATE_HEIGHT, z2);
        return new Plates(left, right, scale, y + PLATE_HEIGHT / 2 + GLYPH_HEIGHT * scale / 2);
    }

    private static boolean spreader(ShuttleLook.Key key) {
        return ModuleSpec.SPREADER.equals(key.tool());
    }
}
