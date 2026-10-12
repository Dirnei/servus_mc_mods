package io.github.dirnei.mc.servus.foundation.side;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The mode of each relative side of one block, limited to the modes the block
 * supports and cycling through them in the declared order. Pure Java.
 */
public final class SideConfig {

    /** Every mode, for machines: Auto, Input, Output, Both, Off, Pull, Push. */
    public static final List<SideMode> MACHINE_MODES = List.of(SideMode.AUTO, SideMode.INPUT, SideMode.OUTPUT, SideMode.BOTH,
            SideMode.DISABLED, SideMode.PULL, SideMode.PUSH);
    /** Input, Pull, Off: a block that only takes items in. */
    public static final List<SideMode> INBOUND_MODES = List.of(SideMode.INPUT, SideMode.PULL, SideMode.DISABLED);
    /** Push, Output, Off: a block that only hands items out. */
    public static final List<SideMode> OUTBOUND_MODES = List.of(SideMode.PUSH, SideMode.OUTPUT, SideMode.DISABLED);
    /** Push, Pull, Off: a block with no item capability that moves items to and from its neighbours itself. */
    public static final List<SideMode> STATION_MODES = List.of(SideMode.PUSH, SideMode.PULL, SideMode.DISABLED);

    private final SideMode[] modes = new SideMode[RelativeSide.values().length];
    private final List<SideMode> supported;
    private final SideMode fallback;
    private final Set<RelativeSide> locked = EnumSet.noneOf(RelativeSide.class);

    /** A machine configuration: every mode, Auto by default. */
    public SideConfig() {
        this(MACHINE_MODES, SideMode.AUTO);
    }

    public SideConfig(List<SideMode> supported, SideMode fallback) {
        if (supported.isEmpty() || !supported.contains(fallback)) {
            throw new IllegalArgumentException("fallback must be one of the supported modes");
        }
        this.supported = List.copyOf(supported);
        this.fallback = fallback;
        Arrays.fill(modes, fallback);
    }

    public static SideConfig machine() {
        return new SideConfig();
    }

    public static SideConfig inbound() {
        return new SideConfig(INBOUND_MODES, SideMode.INPUT);
    }

    public static SideConfig outbound() {
        return new SideConfig(OUTBOUND_MODES, SideMode.DISABLED);
    }

    /** Push, Pull, Off, Push by default: a station that acts on its neighbours itself. */
    public static SideConfig station() {
        return new SideConfig(STATION_MODES, SideMode.PUSH);
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

    /** Fixes a side to one mode: nothing changes it afterwards. */
    public void lock(RelativeSide side, SideMode mode) {
        locked.remove(side);
        set(side, mode);
        locked.add(side);
    }

    public boolean isLocked(RelativeSide side) {
        return locked.contains(side);
    }

    /** Sets a side; an unsupported mode falls back to the default, a locked side stays. */
    public void set(RelativeSide side, SideMode mode) {
        if (locked.contains(side)) {
            return;
        }
        modes[side.ordinal()] = supported.contains(mode) ? mode : fallback;
    }

    /** Advances the side to the next supported mode and returns it; a locked side stays. */
    public SideMode cycle(RelativeSide side) {
        if (locked.contains(side)) {
            return mode(side);
        }
        int index = supported.indexOf(mode(side));
        SideMode next = supported.get((index + 1) % supported.size());
        modes[side.ordinal()] = next;
        return next;
    }

    /** Whether every side that is not locked is at the default. */
    public boolean isDefault() {
        for (RelativeSide side : RelativeSide.values()) {
            if (!locked.contains(side) && modes[side.ordinal()] != fallback) {
                return false;
            }
        }
        return true;
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[modes.length];
        for (int i = 0; i < modes.length; i++) {
            bytes[i] = (byte) modes[i].ordinal();
        }
        return bytes;
    }

    public void fromBytes(byte[] bytes) {
        for (int i = 0; i < modes.length; i++) {
            set(RelativeSide.byOrdinal(i), i < bytes.length ? SideMode.byOrdinal(bytes[i]) : fallback);
        }
    }
}
