package io.github.dirnei.mc.servus.logistics.duct;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Names of the duct part models under {@code models/block/duct/}, shared by
 * datagen and the dynamic model: a centre per line set and an arm per
 * direction and line set. A line set is a mask {@code 1 << line.ordinal()};
 * its suffix joins the line ids in ordinal order with {@code _}, for example
 * {@code item}, {@code energy_fluid} or {@code item_energy_fluid}. Ring faces
 * of the arms carry a tint index of {@link #RING_TINT_BASE} plus line times
 * six plus side, so the block color can look up the channel of that line and
 * side.
 */
public final class DuctParts {

    public static final int RING_TINT_BASE = 100;

    /** Line set masks, {@code 1 << line.ordinal()}. */
    public static final int ITEM = 1 << Line.ITEM.ordinal();
    public static final int ENERGY = 1 << Line.ENERGY.ordinal();
    public static final int FLUID = 1 << Line.FLUID.ordinal();
    /** Every line. */
    public static final int ALL = (1 << Line.VALUES.length) - 1;

    private DuctParts() {
    }

    /** Every non empty line set, in mask order: seven sets for three lines. */
    public static List<Integer> lineSets() {
        List<Integer> sets = new ArrayList<>(ALL);
        for (int mask = 1; mask <= ALL; mask++) {
            sets.add(mask);
        }
        return sets;
    }

    /** The lines of a set in ordinal order. */
    public static List<Line> lines(int linesMask) {
        List<Line> lines = new ArrayList<>(Line.VALUES.length);
        for (Line line : Line.VALUES) {
            if ((linesMask & 1 << line.ordinal()) != 0) {
                lines.add(line);
            }
        }
        return lines;
    }

    /** Masks outside the known lines are cut; an empty set reads as the Item Pipe. */
    public static int normalize(int linesMask) {
        int mask = linesMask & ALL;
        return mask == 0 ? ITEM : mask;
    }

    /** Suffix of a part by line set: the line ids in ordinal order joined with {@code _}. */
    public static String suffix(int linesMask) {
        StringBuilder suffix = new StringBuilder();
        for (Line line : lines(normalize(linesMask))) {
            if (!suffix.isEmpty()) {
                suffix.append('_');
            }
            suffix.append(line.id());
        }
        return suffix.toString();
    }

    /** The centre whose bands run along the Z axis, which also serves single line ducts. */
    public static String centerName(int linesMask) {
        return "center_" + suffix(linesMask);
    }

    /**
     * The centre for a run along the axis; every set of two or more lines has
     * one per axis and, for {@code along == null}, a uniform junction cube.
     */
    public static String centerName(int linesMask, Direction.Axis along) {
        if (Integer.bitCount(normalize(linesMask)) < 2) {
            return centerName(linesMask);
        }
        if (along == null) {
            return centerName(linesMask) + "_junction";
        }
        return switch (along) {
            case X -> centerName(linesMask) + "_x";
            case Y -> centerName(linesMask) + "_y";
            case Z -> centerName(linesMask);
        };
    }

    public static String armName(Direction direction, int linesMask) {
        return "arm_" + direction.getSerializedName() + "_" + suffix(linesMask);
    }

    /** Every part name: per line set the centres and the six arms. */
    public static List<String> allNames() {
        List<String> names = new ArrayList<>();
        for (int lines : lineSets()) {
            names.add(centerName(lines));
            if (Integer.bitCount(lines) >= 2) {
                names.add(centerName(lines, Direction.Axis.X));
                names.add(centerName(lines, Direction.Axis.Y));
                names.add(centerName(lines, null));
            }
            for (Direction direction : Direction.values()) {
                names.add(armName(direction, lines));
            }
        }
        return names;
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(ServusLogisticsIds.MOD_ID, "block/duct/" + name);
    }

    public static int ringTint(Line line, Direction side) {
        return RING_TINT_BASE + line.ordinal() * 6 + side.ordinal();
    }

    /** The line of a ring tint index, or null for another tint index. */
    public static Line ringLine(int tintIndex) {
        int offset = tintIndex - RING_TINT_BASE;
        if (offset < 0 || offset >= Line.VALUES.length * 6) {
            return null;
        }
        return Line.VALUES[offset / 6];
    }

    /** The side of a ring tint index; only meaningful when {@link #ringLine} is not null. */
    public static Direction ringSide(int tintIndex) {
        return Direction.values()[Math.floorMod(tintIndex - RING_TINT_BASE, 6)];
    }
}
