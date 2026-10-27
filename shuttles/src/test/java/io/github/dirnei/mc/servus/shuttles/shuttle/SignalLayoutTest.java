package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.field.HarvesterParts;
import io.github.dirnei.mc.servus.shuttles.shuttle.SignalLayout.Box;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ModuleSpec;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleLook;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.TankerLook;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Lamps and plates against the outlines of the shuttle's part models. The
 * outlines mirror the boxes {@code ShuttleModels} writes; a turned spoiler
 * strut counts as its bounding box.
 */
class SignalLayoutTest {

    /** The shuttle is drawn at 0.8 scale around the block centre, standing 2 pixels up: these model pixels are the block's edges. */
    private static final double BLOCK_MIN = -2;
    private static final double BLOCK_MAX = 18;
    private static final double BLOCK_BOTTOM = -2.5;
    private static final double BLOCK_TOP = 17.5;
    private static final double EPSILON = 1e-9;
    /** Faces closer than this count as one plane. */
    private static final double SAME_PLANE = 0.01;
    private static final String[] TOOLS = {"", ModuleSpec.HARVESTER, ModuleSpec.SPREADER, ModuleSpec.LUMBER, ModuleSpec.TANKER};

    private static List<ShuttleLook.Key> keys() {
        List<ShuttleLook.Key> keys = new ArrayList<>();
        for (int tier = 1; tier <= 3; tier++) {
            for (int speed = 0; speed < ShuttleLook.DRIVE_STEPS; speed++) {
                for (int handling = 0; handling < ShuttleLook.HANDLING_STEPS; handling++) {
                    for (String tool : TOOLS) {
                        keys.add(new ShuttleLook.Key(tier, speed, handling, tool));
                    }
                }
            }
        }
        return keys;
    }

    private static List<Box> signals(ShuttleLook.Key key) {
        SignalLayout.Lamps lamps = SignalLayout.lamps(key);
        List<Box> boxes = new ArrayList<>(lamps.head());
        boxes.addAll(lamps.tail());
        for (int width : new int[] {11, 17, 23, 35, 80}) {
            SignalLayout.Plates plates = SignalLayout.plates(key, width);
            boxes.add(plates.left());
            boxes.add(plates.right());
        }
        return boxes;
    }

    @Test
    void everyLampAndPlateStaysInsideTheRailBlock() {
        for (ShuttleLook.Key key : keys()) {
            for (Box box : signals(key)) {
                assertTrue(box.x1() >= BLOCK_MIN - EPSILON && box.x2() <= BLOCK_MAX + EPSILON
                        && box.z1() >= BLOCK_MIN - EPSILON && box.z2() <= BLOCK_MAX + EPSILON
                        && box.y1() >= BLOCK_BOTTOM - EPSILON && box.y2() <= BLOCK_TOP + EPSILON, key + " " + box);
            }
        }
    }

    @Test
    void lampsStandTheGapOffTheirFaces() {
        for (ShuttleLook.Key key : keys()) {
            SignalLayout.Lamps lamps = SignalLayout.lamps(key);
            assertEquals(2, lamps.head().size());
            assertEquals(2, lamps.tail().size());
            for (Box head : lamps.head()) {
                assertEquals(SignalLayout.FRONT + SignalLayout.GAP, head.z1(), EPSILON, key.toString());
            }
            for (Box tail : lamps.tail()) {
                if (ModuleSpec.SPREADER.equals(key.tool())) {
                    assertEquals(SignalLayout.SPREADER_RIM_TOP + SignalLayout.GAP, tail.y1(), EPSILON, key.toString());
                } else {
                    assertEquals(SignalLayout.BACK - SignalLayout.GAP, tail.z2(), EPSILON, key.toString());
                }
            }
        }
    }

    @Test
    void platesStandTheGapOffBothSides() {
        SignalLayout.Plates plates = SignalLayout.plates(new ShuttleLook.Key(1, 0, 0, ""), 17);
        assertEquals(SignalLayout.LEFT - SignalLayout.GAP, plates.left().x2(), EPSILON);
        assertEquals(SignalLayout.RIGHT + SignalLayout.GAP, plates.right().x1(), EPSILON);
    }

    @Test
    void noLampOrPlateTouchesAPart() {
        for (ShuttleLook.Key key : keys()) {
            List<Box> parts = parts(key);
            for (Box signal : signals(key)) {
                for (Box part : parts) {
                    assertFalse(touches(signal, part), key + ": " + signal + " touches " + part);
                }
            }
        }
    }

    @Test
    void shortNumbersKeepTheirSizeAndLongOnesShrinkToFit() {
        ShuttleLook.Key key = new ShuttleLook.Key(2, 0, 0, "");
        SignalLayout.Plates three = SignalLayout.plates(key, 17);
        SignalLayout.Plates four = SignalLayout.plates(key, 23);
        assertEquals(SignalLayout.TEXT_SCALE, three.scale(), EPSILON);
        assertEquals(SignalLayout.TEXT_SCALE, four.scale(), EPSILON);
        assertTrue(four.right().z2() - four.right().z1() > three.right().z2() - three.right().z1(), "a longer number gets a longer plate");
        assertTrue(four.right().z1() >= SignalLayout.BACK && four.right().z2() <= SignalLayout.FRONT, "#107 fits the body");
        SignalLayout.Plates huge = SignalLayout.plates(key, 80);
        assertTrue(huge.scale() < SignalLayout.TEXT_SCALE, "a very long number is drawn smaller");
        assertEquals(SignalLayout.MAX_PLATE, huge.right().z2() - huge.right().z1(), EPSILON);
    }

    @Test
    void textSitsInTheMiddleOfThePlate() {
        SignalLayout.Plates plates = SignalLayout.plates(new ShuttleLook.Key(1, 0, 0, ""), 17);
        double bottom = plates.textTop() - SignalLayout.GLYPH_HEIGHT * plates.scale();
        assertEquals(plates.right().y2() - plates.textTop(), bottom - plates.right().y1(), EPSILON);
        assertTrue(bottom > plates.right().y1() && plates.textTop() < plates.right().y2());
    }

    /** Whether the boxes overlap or meet so that faces could share a plane. */
    private static boolean touches(Box a, Box b) {
        return a.x1() < b.x2() + SAME_PLANE && b.x1() < a.x2() + SAME_PLANE
                && a.y1() < b.y2() + SAME_PLANE && b.y1() < a.y2() + SAME_PLANE
                && a.z1() < b.z2() + SAME_PLANE && b.z1() < a.z2() + SAME_PLANE;
    }

    private static Box box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new Box(x1, y1, z1, x2, y2, z2);
    }

    /** The part outlines a shuttle with that look is drawn from. */
    private static List<Box> parts(ShuttleLook.Key key) {
        List<Box> parts = new ArrayList<>();
        boolean lumber = ShuttleLook.lumber(key.tool());
        if (lumber) {
            for (HarvesterParts.Box box : HarvesterParts.hold(ShuttleLook.tier(key))) {
                parts.add(box(box.x1(), box.y1(), box.z1(), box.x2(), box.y2(), box.z2()));
            }
        } else {
            body(parts, ShuttleLook.tier(key));
        }
        drive(parts, Math.min(ShuttleLook.DRIVE_STEPS - 1, key.speedStep()), lumber);
        if (!lumber && key.handlingStep() > 0) {
            handling(parts, Math.min(ShuttleLook.HANDLING_STEPS - 1, key.handlingStep()));
        }
        switch (key.tool()) {
            case ModuleSpec.HARVESTER -> harvester(parts);
            case ModuleSpec.SPREADER -> spreader(parts);
            case ModuleSpec.TANKER -> tanker(parts);
            default -> {
            }
        }
        return parts;
    }

    private static void body(List<Box> parts, int tier) {
        parts.add(box(2, 2, 2, 14, 4, 14));
        if (tier == 1) {
            parts.add(box(2, 4, 2, 14, 9, 3));
            parts.add(box(2, 4, 13, 14, 9, 14));
            parts.add(box(2, 4, 3, 3, 9, 13));
            parts.add(box(13, 4, 3, 14, 9, 13));
            return;
        }
        parts.add(box(2, 4, 2, 14, 11, 14));
        if (tier == 3) {
            parts.add(box(2, 11, 2, 14, 12, 14));
        }
    }

    private static void drive(List<Box> parts, int step, boolean lumber) {
        switch (step) {
            case 0 -> wheels(parts, 4, 3, 9);
            case 1 -> wheels(parts, 3.5, 2.25, 6.25, 10.25);
            default -> {
                parts.add(box(1.75, 0, 2.05, 4.25, 3, 13.95));
                parts.add(box(11.75, 0, 2.05, 14.25, 3, 13.95));
                parts.add(box(1.25, 1, 3.5, 1.75, 2, 12.5));
                parts.add(box(14.25, 1, 3.5, 14.75, 2, 12.5));
            }
        }
        if (step < 3) {
            return;
        }
        parts.add(box(0.5, 14, -1, 15.5, 15, 2.5));
        if (lumber) {
            parts.add(box(0, 2, -1.5, 0.5, 15.5, 2.7));
            parts.add(box(15.5, 2, -1.5, 16, 15.5, 2.7));
            parts.add(box(0.5, 2.5, 2.1, 2, 3.5, 2.7));
            parts.add(box(14, 2.5, 2.1, 15.5, 3.5, 2.7));
        } else {
            parts.add(box(0, 12.5, -1.5, 0.5, 15.5, 3));
            parts.add(box(15.5, 12.5, -1.5, 16, 15.5, 3));
            parts.add(box(5.5, 8.8, 0.3, 6.5, 14.9, 3.5));
            parts.add(box(9.5, 8.8, 0.3, 10.5, 14.9, 3.5));
        }
    }

    private static void wheels(List<Box> parts, double diameter, double... zs) {
        for (double z : zs) {
            parts.add(box(0.5, 0, z, 2, diameter, z + diameter));
            parts.add(box(14, 0, z, 15.5, diameter, z + diameter));
            parts.add(box(2, 0, z + 0.5, 4, 2, z + diameter - 0.5));
            parts.add(box(12, 0, z + 0.5, 14, 2, z + diameter - 0.5));
        }
    }

    private static void handling(List<Box> parts, int step) {
        double height = step == 1 ? 9 : 13;
        parts.add(box(3, 1, 1, 4, height, 2));
        parts.add(box(12, 1, 1, 13, height, 2));
        parts.add(box(4, 3, 1, 12, 4, 2));
        parts.add(box(5, 0.5, 0, 6, 1.5, 2));
        parts.add(box(10, 0.5, 0, 11, 1.5, 2));
        if (step == 2) {
            parts.add(box(7, 12, 1, 9, 13, 10));
            parts.add(box(7.5, 10, 9, 8.5, 12, 10));
        }
    }

    private static void harvester(List<Box> parts) {
        parts.add(box(2.05, 3.05, 13.05, 3, 5.95, 16.95));
        parts.add(box(13, 3.05, 13.05, 13.95, 5.95, 16.95));
        parts.add(box(-2, 3, 15, 18, 7, 17));
        parts.add(box(-2, 0.5, 15, 18, 1.5, 18));
    }

    private static void spreader(List<Box> parts) {
        parts.add(box(2.05, 3, -1, 3, 6, 2.5));
        parts.add(box(13, 3, -1, 13.95, 6, 2.5));
        parts.add(box(3, 6, -4, 13, 11, 0));
        parts.add(box(2.5, 11, -4.5, 13.5, 12, 0));
        parts.add(box(5, 1.5, -3, 11, 6, -1));
        parts.add(box(-2, 0.5, -3.5, 18, 3, -1.5));
    }

    private static void tanker(List<Box> parts) {
        parts.add(box(4.3, 4.05, 2.6, 11.7, TankerLook.CAP_TOP, 13.4));
        parts.add(box(TankerLook.GLASS_LEFT, 11.75, 6, 4.6, 14.25, 10));
        parts.add(box(11.4, 11.75, 6, TankerLook.GLASS_RIGHT, 14.25, 10));
        double y0 = TankerLook.COUPLING_Y - TankerLook.COLLAR_HALF;
        double y1 = TankerLook.COUPLING_Y + TankerLook.COLLAR_HALF;
        double z0 = TankerLook.COUPLING_Z - TankerLook.COLLAR_HALF;
        double z1 = TankerLook.COUPLING_Z + TankerLook.COLLAR_HALF;
        parts.add(box(TankerLook.COLLAR_X0, y0, z0, TankerLook.COLLAR_X1, y1, z1));
        parts.add(box(16 - TankerLook.COLLAR_X1, y0, z0, 16 - TankerLook.COLLAR_X0, y1, z1));
    }
}
