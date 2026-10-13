package io.github.dirnei.mc.servus.foundation.side;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * The Fluids layer of a side configuration: per relative side Off, All, or one of the
 * block's tanks. New sides are All, which exposes every tank; locked sides stay Off. Values are plain ints: {@link #OFF}, {@link #ALL}, or
 * {@link #TANK_FIRST} plus the tank index.
 */
public final class FluidSides {

    public static final int OFF = 0;
    public static final int ALL = 1;
    public static final int TANK_FIRST = 2;

    private final int tanks;
    private final int[] values = new int[RelativeSide.values().length];
    private final Set<RelativeSide> locked = EnumSet.noneOf(RelativeSide.class);

    public FluidSides(int tanks) {
        if (tanks < 1) {
            throw new IllegalArgumentException("a Fluids layer needs at least one tank");
        }
        this.tanks = tanks;
        Arrays.fill(values, ALL);
    }

    public int tanks() {
        return tanks;
    }

    /** The raw value of a side. */
    public int value(RelativeSide side) {
        return values[side.ordinal()];
    }

    public boolean isOff(RelativeSide side) {
        return value(side) == OFF;
    }

    public boolean isAll(RelativeSide side) {
        return value(side) == ALL;
    }

    /** The tank index a side is set to, or -1 for Off and All. */
    public int tank(RelativeSide side) {
        int value = value(side);
        return value >= TANK_FIRST ? value - TANK_FIRST : -1;
    }

    /** Whether the side exposes the tank: set to it or to All. */
    public boolean exposes(RelativeSide side, int tank) {
        return isAll(side) || tank(side) == tank;
    }

    /** Fixes a side to Off: nothing changes it afterwards. */
    public void lock(RelativeSide side) {
        values[side.ordinal()] = OFF;
        locked.add(side);
    }

    public boolean isLocked(RelativeSide side) {
        return locked.contains(side);
    }

    /** Sets a side; a value outside the known ones becomes All, a locked side stays. */
    public void set(RelativeSide side, int value) {
        if (locked.contains(side)) {
            return;
        }
        values[side.ordinal()] = valid(value) ? value : ALL;
    }

    /** Advances Off, All, tank 0 .. tank n-1, Off and returns the new value; a locked side stays. */
    public int cycle(RelativeSide side) {
        if (locked.contains(side)) {
            return value(side);
        }
        int next = (value(side) + 1) % (TANK_FIRST + tanks);
        values[side.ordinal()] = next;
        return next;
    }

    /** Whether every side that is not locked is All. */
    public boolean isDefault() {
        for (RelativeSide side : RelativeSide.values()) {
            if (!locked.contains(side) && values[side.ordinal()] != ALL) {
                return false;
            }
        }
        return true;
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

    /** Reads saved values; missing or unknown entries become All, locked sides stay Off. */
    public void fromBytes(byte[] bytes) {
        for (int i = 0; i < values.length; i++) {
            set(RelativeSide.byOrdinal(i), i < bytes.length ? bytes[i] : ALL);
        }
    }

    private boolean valid(int value) {
        return value >= OFF && value < TANK_FIRST + tanks;
    }
}
