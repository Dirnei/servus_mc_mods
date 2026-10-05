package io.github.dirnei.mc.servus.lib.side;

import java.util.Arrays;

/** The mode of each relative side of one block. Pure Java. */
public final class SideConfig {

    private final SideMode[] modes = new SideMode[RelativeSide.values().length];

    public SideConfig() {
        Arrays.fill(modes, SideMode.AUTO);
    }

    public SideMode mode(RelativeSide side) {
        return modes[side.ordinal()];
    }

    public void set(RelativeSide side, SideMode mode) {
        modes[side.ordinal()] = mode;
    }

    public SideMode cycle(RelativeSide side) {
        SideMode next = mode(side).next();
        set(side, next);
        return next;
    }

    public boolean isDefault() {
        for (SideMode mode : modes) {
            if (mode != SideMode.AUTO) {
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
            modes[i] = i < bytes.length ? SideMode.byOrdinal(bytes[i]) : SideMode.AUTO;
        }
    }
}
