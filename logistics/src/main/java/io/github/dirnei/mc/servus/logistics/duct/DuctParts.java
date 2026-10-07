package io.github.dirnei.mc.servus.logistics.duct;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Names of the duct part models under {@code models/block/duct/}, shared by
 * datagen and the dynamic model: a centre per line set and an arm per
 * direction and line set. Ring faces of the arms carry a tint index of
 * {@link #RING_TINT_BASE} plus line times six plus side, so the block color
 * can look up the channel of that line and side.
 */
public final class DuctParts {

    public static final int RING_TINT_BASE = 100;

    /** Line set masks, {@code 1 << line.ordinal()}: the Item Pipe is ordinal 0, the Power Cable 1. */
    public static final int ITEM = 1;
    public static final int ENERGY = 2;
    public static final int BOTH = ITEM | ENERGY;

    static {
        if (1 << Line.ITEM.ordinal() != ITEM || 1 << Line.ENERGY.ordinal() != ENERGY) {
            throw new IllegalStateException("Line ordinals changed; update the DuctParts masks");
        }
    }

    private DuctParts() {
    }

    /** Suffix of a part by line set: {@code item}, {@code energy} or {@code both}. */
    public static String suffix(int linesMask) {
        return switch (linesMask & BOTH) {
            case ENERGY -> "energy";
            case BOTH -> "both";
            default -> "item";
        };
    }

    public static String centerName(int linesMask) {
        return "center_" + suffix(linesMask);
    }

    public static String armName(Direction direction, int linesMask) {
        return "arm_" + direction.getSerializedName() + "_" + suffix(linesMask);
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
