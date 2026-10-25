package io.github.dirnei.mc.servus.shuttles.field;

import java.util.ArrayList;
import java.util.List;

/**
 * The boxes of a Lumber shuttle's hold, crane and harvester head, in the
 * shuttle model's pixels (+z the front, the body spans 2 to 14). Parts the
 * renderer only moves lie where they sit at rest; parts it turns are built
 * around their own origin, which their model shifts to {@link #CENTRE}.
 * Datagen writes them as models, {@link HarvesterPose} places them. Pure Java.
 */
public final class HarvesterParts {

    /** Where a centred part's origin lies in its model. */
    public static final double CENTRE = 8;
    /** The top of the walls' rim; the lid doors' panels and the Mk3 gold corners reach 0.05 higher. */
    public static final double HOLD_TOP = 12.45;
    /** The highest point of the closed hold, under the garage doorway. */
    public static final double TOP = 12.5;
    /** The bottom and top of a lid door's leaf, sunk in the rim; its dark panel stands on it up to {@link #TOP}. */
    public static final double DOOR_BOTTOM = HOLD_TOP - 0.85;
    public static final double DOOR_TOP = HOLD_TOP - 0.15;
    /** The walls hinge on their outer bottom edge at the chassis top. */
    public static final double HINGE_Y = 4;
    /** The outer face of the right wall and of the front wall; the left and back are these turned half round. */
    public static final double OUTER = 14;
    public static final double INNER = 13.25;
    /** The pillar's foot on the ring and its top while the crane is down. */
    public static final double PILLAR_FOOT = 5.5;
    public static final double PILLAR_LOW_TOP = 9;
    /** The collar's centre height while the crane is down. */
    public static final double COLLAR_LOW = 8.5;
    /** Model lengths of the parts the renderer stretches to the pose's length. */
    public static final double BOOM_LENGTH = 9;
    public static final double CYLINDER_LENGTH = 5;
    public static final double STICK_LENGTH = 9;
    public static final double TELESCOPE_LENGTH = 8;
    /** The head reaches round a log one block wide: its near face this far in front of the rotator, half its width across. */
    public static final double LOG_NEAR = 2;
    public static final double LOG_HALF = 10;
    /** The arms hinge on the frame's front corners, this far out and forward of the rotator. */
    public static final double ARM_HINGE_X = 3.5;
    public static final double ARM_HINGE_Z = 1.5;
    /** The inner link's length from hinge to elbow, for the roller arm and the knife arms. */
    public static final double ROLLER_INNER = LOG_HALF + 3.5 - 4.1;
    public static final double KNIFE_INNER = LOG_HALF + 0.25 - 4.1;
    /** The bottom and top of each arm under the rotator: the upper knife, the roller and the lower knife. */
    public static final double[][] KNIVES = {{-4.75, -3.75}, {-11.5, -10.5}};
    public static final double[] ROLLER = {-9.75, -5.25};
    /** The outer links are this much lower than the inner ones at top and bottom, so the two never share a plane where they cross. */
    private static final double LINK_INSET = 0.05;
    /** The chain bar swings about this point under the frame. */
    public static final double[] BAR_PIVOT = {-6.5, -12.75, 1};

    /** What a box is painted with; datagen picks the texture, the hold and rim by tier. */
    public enum Paint { HOLD, RIM, CHASSIS, DARK, STEEL, ARM, RUBBER, GOLD }

    /** One box from its low to its high corner. */
    public record Box(double x1, double y1, double z1, double x2, double y2, double z2, Paint paint) {

        /** A box with its corners given in any order. */
        public static Box of(double xa, double ya, double za, double xb, double yb, double zb, Paint paint) {
            return new Box(Math.min(xa, xb), Math.min(ya, yb), Math.min(za, zb), Math.max(xa, xb), Math.max(ya, yb), Math.max(za, zb),
                    paint);
        }

        /** The box mirrored across x 0. */
        Box mirrored() {
            return new Box(-x2, y1, z1, -x1, y2, z2, paint);
        }

        /** The box turned half round the hold's vertical centre line. */
        Box turned() {
            return new Box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1, paint);
        }
    }

    /** Each part the renderer draws, its model name and whether it is per tier or built around its own origin. */
    public enum Part {
        BODY("lumber_body", true, false), SIDE("lumber_side", true, false), END("lumber_end", true, false),
        DOOR("lumber_door", true, false), RING("lumber_ring", false, false), PILLAR("lumber_pillar", false, false),
        COLLAR("lumber_collar", false, false), BOOM("lumber_boom", false, true), CYLINDER("lumber_cylinder", false, true),
        KNUCKLE("lumber_knuckle", false, true), STICK("lumber_stick", false, true), TELESCOPE("lumber_telescope", false, true),
        ROTATOR("lumber_rotator", false, true), FRAME("lumber_frame", false, true), SAW_BOX("lumber_saw_box", false, true),
        BAR("lumber_bar", false, true), ROLLER_INNER("lumber_roller_inner", false, true),
        ROLLER_OUTER("lumber_roller_outer", false, true), KNIFE_INNER("lumber_knife_inner", false, true),
        KNIFE_OUTER("lumber_knife_outer", false, true), ROLLER_INNER_LEFT("lumber_roller_inner_left", false, true),
        ROLLER_OUTER_LEFT("lumber_roller_outer_left", false, true), KNIFE_INNER_LEFT("lumber_knife_inner_left", false, true),
        KNIFE_OUTER_LEFT("lumber_knife_outer_left", false, true);

        private final String name;
        private final boolean tiered;
        private final boolean centred;

        Part(String name, boolean tiered, boolean centred) {
            this.name = name;
            this.tiered = tiered;
            this.centred = centred;
        }

        /** The model name, {@code lumber_<part>} or {@code lumber_<part>_mk<tier>}. */
        public String model(int tier) {
            return tiered ? name + "_mk" + tier : name;
        }

        public boolean tiered() {
            return tiered;
        }

        /** Whether the part is built around its origin, which its model shifts to {@link #CENTRE}. */
        public boolean centred() {
            return centred;
        }
    }

    private HarvesterParts() {
    }

    /** The part's boxes; the tier matters for the hold's parts only. */
    public static List<Box> boxes(Part part, int tier) {
        return switch (part) {
            case BODY -> body();
            case SIDE -> side();
            case END -> end(tier);
            case DOOR -> door();
            case RING -> List.of(new Box(5.5, 4.75, 9, 10.5, 5.5, 13.2, Paint.DARK));
            case PILLAR -> List.of(new Box(6.55, PILLAR_FOOT, 10, 9.45, PILLAR_LOW_TOP, 13, Paint.ARM));
            case COLLAR -> List.of(new Box(6, COLLAR_LOW - 1, 10.5, 10, COLLAR_LOW + 1, 12.5, Paint.DARK));
            case BOOM -> List.of(segment(3, BOOM_LENGTH, Paint.ARM));
            case CYLINDER -> List.of(segment(1.2, CYLINDER_LENGTH, Paint.STEEL));
            case KNUCKLE -> List.of(new Box(-1.75, -1.75, -1.75, 1.75, 1.75, 1.75, Paint.DARK));
            case STICK -> List.of(segment(2.5, STICK_LENGTH, Paint.ARM));
            case TELESCOPE -> List.of(segment(1.8, TELESCOPE_LENGTH, Paint.STEEL));
            case ROTATOR -> List.of(new Box(-1.5, -2, -1.5, 1.5, 0, 1.5, Paint.DARK), new Box(-2.5, -3.5, -2, 2.5, -2, 2, Paint.DARK));
            case FRAME -> List.of(new Box(-3.5, -12, -2.5, 3.5, -3.5, 1.5, Paint.ARM), new Box(-2.5, -11, -3, 2.5, -4.5, -2.5, Paint.DARK),
                    new Box(-0.4, -10, 1.5, 0.4, -8, 2.5, Paint.DARK));
            case SAW_BOX -> List.of(new Box(-7, -13.5, -2.5, 3, -12, 1.5, Paint.DARK));
            case BAR -> List.of(new Box(0, -0.45, -0.7, 12.4, 0.45, 0.7, Paint.STEEL), new Box(0, -0.6, 0.7, 13, 0.6, 1.1, Paint.DARK),
                    new Box(0, -0.6, -1.1, 13, 0.6, -0.7, Paint.DARK), new Box(12.4, -0.6, -0.7, 13, 0.6, 0.7, Paint.DARK));
            case ROLLER_INNER -> innerLink(ROLLER_INNER, ROLLER[1] - ROLLER[0], Paint.ARM);
            case ROLLER_OUTER -> rollerOuter();
            case KNIFE_INNER -> innerLink(KNIFE_INNER, KNIVES[0][1] - KNIVES[0][0], Paint.STEEL);
            case KNIFE_OUTER -> knifeOuter();
            case ROLLER_INNER_LEFT -> mirrored(boxes(Part.ROLLER_INNER, tier));
            case ROLLER_OUTER_LEFT -> mirrored(boxes(Part.ROLLER_OUTER, tier));
            case KNIFE_INNER_LEFT -> mirrored(boxes(Part.KNIFE_INNER, tier));
            case KNIFE_OUTER_LEFT -> mirrored(boxes(Part.KNIFE_OUTER, tier));
        };
    }

    /** The closed Lumber hold at rest: the body, both walls, both ends and both doors in place. */
    public static List<Box> hold(int tier) {
        List<Box> boxes = new ArrayList<>(body());
        for (Part part : List.of(Part.SIDE, Part.END, Part.DOOR)) {
            for (Box box : boxes(part, tier)) {
                boxes.add(box);
                boxes.add(box.turned());
            }
        }
        return boxes;
    }

    /** The chassis and the hold's floor; the walls fold away from it. */
    private static List<Box> body() {
        return List.of(new Box(2, 2, 2, 14, 4, 14, Paint.CHASSIS), new Box(2.75, 4, 2.75, 13.25, 4.75, 13.25, Paint.HOLD));
    }

    /** The right wall with its rim strip and two dark stabilizer strips on the outside. */
    private static List<Box> side() {
        return List.of(Box.of(OUTER, HINGE_Y, 2.75, INNER, HOLD_TOP - 1, 13.25, Paint.HOLD),
                Box.of(OUTER, HOLD_TOP - 1, 2.75, INNER, HOLD_TOP, 13.25, Paint.RIM),
                Box.of(OUTER, 4.2, 4, OUTER + 0.3, 6, 12, Paint.DARK),
                Box.of(OUTER, HOLD_TOP - 1.6, 3.5, OUTER + 0.4, HOLD_TOP - 0.2, 12.5, Paint.DARK));
    }

    /** The front wall with the rim across its top and over both corners, its dark strips and the Mk3 gold corners. */
    private static List<Box> end(int tier) {
        List<Box> boxes = new ArrayList<>(List.of(Box.of(2.75, HINGE_Y, OUTER, 13.25, HOLD_TOP - 1, INNER, Paint.HOLD),
                Box.of(2, HOLD_TOP - 1, OUTER, 14, HOLD_TOP, INNER, Paint.RIM),
                Box.of(4, 4.2, OUTER, 12, 6, OUTER + 0.3, Paint.DARK),
                Box.of(3.5, HOLD_TOP - 1.6, OUTER, 12.5, HOLD_TOP - 0.2, OUTER + 0.4, Paint.DARK)));
        if (tier == 3) {
            for (double x : new double[] {2, 12}) {
                boxes.add(Box.of(x - 0.05, HOLD_TOP - 1.05, OUTER + 0.05, x + 2.05, TOP, INNER - 1.25, Paint.GOLD));
            }
        }
        return boxes;
    }

    /** The right lid door, hinged on the right wall's rim, with its dark panel on top. */
    private static List<Box> door() {
        return List.of(Box.of(INNER, DOOR_BOTTOM, 2.75, 8, DOOR_TOP, 13.25, Paint.HOLD),
                Box.of(INNER - 1, DOOR_TOP, 4, 8.75, TOP, 12, Paint.RIM));
    }

    /** A box of square section {@code width} along +z from 0 to the length. */
    private static Box segment(double width, double length, Paint paint) {
        return new Box(-width / 2, -width / 2, 0, width / 2, width / 2, length, paint);
    }

    /** An inner link from the hinge out along +x to the elbow's dark joint, its height from y 0. */
    private static List<Box> innerLink(double length, double height, Paint paint) {
        return List.of(new Box(-0.5, 0, -0.5, length + 0.5, height, 0.5, paint),
                new Box(length - 0.6, -0.3, -0.6, length + 0.6, height + 0.3, 0.6, Paint.DARK));
    }

    /** The roller arm's outer link from the elbow along the log, a plate at its end and the spiked feed roller inside it. */
    private static List<Box> rollerOuter() {
        double height = ROLLER[1] - ROLLER[0];
        double length = LOG_NEAR + LOG_HALF;
        double cx = 0.6 - 1.75;
        double cy = height / 2;
        double cz = 6;
        double r = 1.75;
        return List.of(new Box(0.6, LINK_INSET, -0.6, 1.6, height - LINK_INSET, length, Paint.DARK),
                new Box(-1.4, LINK_INSET, length - 1, 0.6, height - LINK_INSET, length, Paint.DARK),
                new Box(cx - r, cy - r * 1.2, cz - 1.75, cx + r, cy + r * 1.2, cz + 1.75, Paint.RUBBER),
                new Box(cx - r * 0.7, cy - r * 1.5, cz - 1.7, cx + r * 0.7, cy + r * 1.5, cz + 1.7, Paint.RUBBER),
                new Box(cx - 0.5, cy - 0.5, cz - 2.25, cx + 0.5, cy + 0.5, cz + 2.25, Paint.DARK));
    }

    /** A knife arm's outer link along the log with the hooked knife at its end. */
    private static List<Box> knifeOuter() {
        double height = KNIVES[0][1] - KNIVES[0][0];
        double length = LOG_NEAR + LOG_HALF;
        return List.of(new Box(0.6, LINK_INSET, -0.6, 1.35, height - LINK_INSET, length, Paint.STEEL),
                new Box(-4.9, LINK_INSET, length, 1.35, height - LINK_INSET, length + 1, Paint.STEEL),
                new Box(-4.9, LINK_INSET, length - 1.25, -3.9, height - LINK_INSET, length, Paint.STEEL));
    }

    private static List<Box> mirrored(List<Box> boxes) {
        return boxes.stream().map(Box::mirrored).toList();
    }
}
