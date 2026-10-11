package io.github.dirnei.mc.servus.storage.shuttle;

import java.util.OptionalInt;
import java.util.function.IntPredicate;

/**
 * Speed upgrades of an elevator shaft: what they do to its car and which
 * segment of a column keeps them. Segments are given by y in one column.
 * Pure, no Minecraft imports.
 */
public final class LiftUpgrades {

    private LiftUpgrades() {
    }

    /** The car's top speed in blocks per tick with that many upgrades. */
    public static double speed(double base, double perUpgrade, int count) {
        return base + perUpgrade * Math.max(0, count);
    }

    /** The speed the car gains per tick when it starts, and loses when it brakes, with that many upgrades. */
    public static double accel(double base, double perUpgrade, int count) {
        return base + perUpgrade * Math.max(0, count);
    }

    /** The lowest segment of the shaft the segment at y belongs to. */
    public static int bottom(int y, IntPredicate segment) {
        int bottom = y;
        while (segment.test(bottom - 1)) {
            bottom--;
        }
        return bottom;
    }

    /** Where the upgrades of the segment at y go when it is removed: the segment above, or nowhere, so they drop. */
    public static OptionalInt heirOnRemove(int y, IntPredicate segment) {
        return segment.test(y + 1) ? OptionalInt.of(y + 1) : OptionalInt.empty();
    }
}
