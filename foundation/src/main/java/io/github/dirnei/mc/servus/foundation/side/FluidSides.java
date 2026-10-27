package io.github.dirnei.mc.servus.foundation.side;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The Fluids layer of a side configuration: per relative side a {@link SideMode} out of the
 * block's supported list and a tank choice, {@link #ALL} or one tank index. New sides are the
 * fallback mode on All; locked sides stay Off on All.
 *
 * <p>A side packs into one byte: the mode ordinal in the low four bits and the tank choice
 * plus one in the high four bits, so 0 there means All. That allows up to {@link #MAX_TANKS} tanks.
 */
public final class FluidSides {

    /** The tank choice of a side that reaches every tank. */
    public static final int ALL = -1;
    public static final int MAX_TANKS = 15;

    private final int tanks;
    private final List<SideMode> supported;
    private final SideMode fallback;
    private final SideMode[] modes = new SideMode[RelativeSide.values().length];
    private final int[] tankChoices = new int[RelativeSide.values().length];
    private final Set<RelativeSide> locked = EnumSet.noneOf(RelativeSide.class);

    public FluidSides(int tanks, List<SideMode> supported, SideMode fallback) {
        if (tanks < 1 || tanks > MAX_TANKS) {
            throw new IllegalArgumentException("a Fluids layer needs 1 to " + MAX_TANKS + " tanks");
        }
        if (supported.isEmpty() || !supported.contains(fallback)) {
            throw new IllegalArgumentException("fallback must be one of the supported modes");
        }
        this.tanks = tanks;
        this.supported = List.copyOf(supported);
        this.fallback = fallback;
        Arrays.fill(modes, fallback);
        Arrays.fill(tankChoices, ALL);
    }

    public int tanks() {
        return tanks;
    }

    public List<SideMode> supported() {
        return supported;
    }

    public SideMode fallback() {
        return fallback;
    }

    public SideMode mode(RelativeSide side) {
        return modes[side.ordinal()];
    }

    /** The tank index a side is set to, or {@link #ALL}. */
    public int tank(RelativeSide side) {
        return tankChoices[side.ordinal()];
    }

    /** Whether the side's tank choice includes the tank: set to it or to All. */
    public boolean reaches(RelativeSide side, int tank) {
        int choice = tank(side);
        return choice == ALL || choice == tank;
    }

    /** Sets a side's mode; an unsupported mode falls back to the default, a locked side stays. */
    public void setMode(RelativeSide side, SideMode mode) {
        if (locked.contains(side)) {
            return;
        }
        modes[side.ordinal()] = supported.contains(mode) ? mode : fallback;
    }

    /** Sets a side's tank choice; an unknown tank becomes All, a locked side stays. */
    public void setTank(RelativeSide side, int tank) {
        if (locked.contains(side)) {
            return;
        }
        tankChoices[side.ordinal()] = tank >= 0 && tank < tanks ? tank : ALL;
    }

    /** Advances the side to the next supported mode, keeping its tank, and returns it; a locked side stays. */
    public SideMode cycleMode(RelativeSide side) {
        if (locked.contains(side)) {
            return mode(side);
        }
        int index = supported.indexOf(mode(side));
        SideMode next = supported.get((index + 1) % supported.size());
        modes[side.ordinal()] = next;
        return next;
    }

    /** Advances the tank choice All, 0 .. n-1, All, keeping the mode, and returns it; a locked side stays. */
    public int cycleTank(RelativeSide side) {
        if (locked.contains(side)) {
            return tank(side);
        }
        int next = tank(side) + 1 >= tanks ? ALL : tank(side) + 1;
        tankChoices[side.ordinal()] = next;
        return next;
    }

    /** Fixes a side to Off on All: nothing changes it afterwards. */
    public void lock(RelativeSide side) {
        modes[side.ordinal()] = SideMode.DISABLED;
        tankChoices[side.ordinal()] = ALL;
        locked.add(side);
    }

    public boolean isLocked(RelativeSide side) {
        return locked.contains(side);
    }

    /** Whether every side that is not locked is the fallback mode on All. */
    public boolean isDefault() {
        for (RelativeSide side : RelativeSide.values()) {
            if (!locked.contains(side) && (mode(side) != fallback || tank(side) != ALL)) {
                return false;
            }
        }
        return true;
    }

    /** The packed byte of one side, for menu data slots and saving. */
    public int packed(RelativeSide side) {
        return pack(mode(side), tank(side));
    }

    /** Sets a side from a packed byte; unknown parts fall back like {@link #setMode} and {@link #setTank}. */
    public void setPacked(RelativeSide side, int packed) {
        setMode(side, modeOf(packed));
        setTank(side, tankOf(packed));
    }

    public static int pack(SideMode mode, int tank) {
        return ((tank + 1) & 0xF) << 4 | (mode.ordinal() & 0xF);
    }

    /** The mode of a packed byte, or null when the ordinal is unknown. */
    public static SideMode modeOf(int packed) {
        int ordinal = packed & 0xF;
        return ordinal < SideMode.values().length ? SideMode.byOrdinal(ordinal) : null;
    }

    /** The tank choice of a packed byte, {@link #ALL} or an index. */
    public static int tankOf(int packed) {
        return ((packed >> 4) & 0xF) - 1;
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[modes.length];
        for (RelativeSide side : RelativeSide.values()) {
            bytes[side.ordinal()] = (byte) packed(side);
        }
        return bytes;
    }

    /** Reads saved sides; missing or unknown entries become the fallback on All, locked sides stay Off. */
    public void fromBytes(byte[] bytes) {
        for (RelativeSide side : RelativeSide.values()) {
            int i = side.ordinal();
            setPacked(side, i < bytes.length ? bytes[i] & 0xFF : pack(fallback, ALL));
        }
    }
}
