package io.github.dirnei.mc.servus.brewery.brewhouse;

import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer.Part;
import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer.Side;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The parts of the formed Brewhouse in structure pixels (see {@link ModelSlicer}), after the approved mockup: a low
 * brick firebox, two iron fire doors on the front, the copper kettle with its dome and vent pipe on the left and the
 * kiln tray with its grate on the right. Pure Java. Texture names are model texture variables.
 */
public final class BrewhouseModel {

    public static final String BRICKS = "#bricks";
    public static final String IRON = "#iron";
    public static final String DOOR = "#door";
    public static final String COPPER = "#copper";
    public static final String TRAY = "#tray";
    public static final String GRATE = "#grate";
    public static final String GAUGE = "#gauge";
    public static final String PORT = "#port";

    /** Height of the firebox in pixels. */
    public static final float FIREBOX_TOP = 12;
    /** How far the doors stand out of the front. */
    public static final float DOOR_DEPTH = 0.5F;
    /** Top of the vent pipe: the highest a kettle block's model may reach. */
    public static final float PIPE_TOP = 48;
    /** Middle of the vent pipe. */
    public static final float PIPE_U = 16;
    public static final float PIPE_V = 16;

    public static final Part BRICK_BODY = part("bricks", 0, 0, 0, 48, 32, FIREBOX_TOP, all(BRICKS), false);
    public static final Part KETTLE_DOOR = door("kettle_door", 8, 1, 24, 11);
    public static final Part KILN_DOOR = door("kiln_door", 34, 2, 46, 9);
    public static final Part KETTLE = part("kettle", 2, 2, FIREBOX_TOP, 30, 30, 30, resting(COPPER), false);
    public static final Part DOME = part("dome", 6, 6, 30, 26, 26, 34, resting(COPPER), false);
    public static final Part PIPE = part("pipe", 14, 14, 34, 18, 18, PIPE_TOP, resting(COPPER), false);
    public static final Part TRAY_PAN = tray();
    public static final Part TRAY_GRATE = part("grate", 33, 2, 15, 47, 30, 15.5F, resting(GRATE), false);

    /** Every part of the formed Brewhouse. */
    public static final List<Part> PARTS = List.of(BRICK_BODY, KETTLE_DOOR, KILN_DOOR, KETTLE, DOME, PIPE, TRAY_PAN, TRAY_GRATE);

    /** The round thermometer between the doors, shown with a Heating Element. */
    public static final Part GAUGE_DIAL = gauge();
    /** The flat iron power port on the right end of the back kiln brick, shown with a Heating Element. */
    public static final Part POWER_PORT = port();
    /** The parts a Heating Element adds; they take no part in the block shapes. */
    public static final List<Part> HEATER_PARTS = List.of(GAUGE_DIAL, POWER_PORT);
    /** Middle of the thermometer's face, where the needle turns. */
    public static final float GAUGE_U = 29;
    public static final float GAUGE_Y = 6;

    private BrewhouseModel() {
    }

    private static Part part(String name, float u0, float v0, float y0, float u1, float v1, float y1, Map<Side, String> textures,
                             boolean stretched) {
        return new Part(name, u0, v0, y0, u1, v1, y1, textures, stretched);
    }

    /** An iron door plate standing out of the front; its back lies on the bricks and is not drawn. */
    private static Part door(String name, float u0, float y0, float u1, float y1) {
        Map<Side, String> textures = new EnumMap<>(Side.class);
        textures.put(Side.FRONT, DOOR);
        textures.put(Side.LEFT, IRON);
        textures.put(Side.RIGHT, IRON);
        textures.put(Side.UP, IRON);
        textures.put(Side.DOWN, IRON);
        return part(name, u0, -DOOR_DEPTH, y0, u1, 0, y1, textures, true);
    }

    /**
     * The UVs of a side that do not follow the position rule, or null: the whole port texture on the port's face, and
     * on the thermometer's edges the dial's outer ring (half a pixel of column 1 or 14, row 1 or 14), so they show brass
     * only where the round dial reaches them.
     */
    public static float[] fixedUv(Part part, Side side) {
        if (part == POWER_PORT && side == Side.RIGHT) {
            return new float[] {0, 0, 16, 16};
        }
        if (part != GAUGE_DIAL) {
            return null;
        }
        return switch (side) {
            case LEFT -> new float[] {1, 0, 1.5F, 16};
            case RIGHT -> new float[] {14, 0, 14.5F, 16};
            case UP -> new float[] {0, 1, 16, 1.5F};
            case DOWN -> new float[] {0, 14, 16, 14.5F};
            default -> null;
        };
    }

    private static Part gauge() {
        Map<Side, String> textures = all(GAUGE);
        textures.remove(Side.BACK);
        return part("gauge", 26.5F, -0.25F, 3.5F, 31.5F, 0, 8.5F, textures, true);
    }

    private static Part port() {
        Map<Side, String> textures = all(IRON);
        textures.remove(Side.LEFT);
        textures.put(Side.RIGHT, PORT);
        return part("port", 48, 21, 3, 49, 27, 9, textures, false);
    }

    private static Part tray() {
        Map<Side, String> textures = resting(IRON);
        textures.put(Side.UP, TRAY);
        return part("tray", 33, 2, FIREBOX_TOP, 47, 30, 15, textures, false);
    }

    private static Map<Side, String> all(String texture) {
        Map<Side, String> textures = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            textures.put(side, texture);
        }
        return textures;
    }

    /** Every side but the bottom, for a part that stands on another. */
    private static Map<Side, String> resting(String texture) {
        Map<Side, String> textures = all(texture);
        textures.remove(Side.DOWN);
        return textures;
    }
}
