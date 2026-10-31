package io.github.dirnei.mc.servus.shuttles.locker;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * What the screen above a Parcel Locker's dock shows: the QR code with a
 * blinking SCAN, the greeting after a scan that opened boxes, no parcel after
 * a scan that opened none, LOAD while shuttles bring orders, and NOT YOURS
 * after a click on someone else's box. HELLO, NONE and DENY fall back after a
 * few seconds.
 */
public enum LockerScreen implements StringRepresentable {
    IDLE,
    HELLO,
    NONE,
    LOAD,
    DENY;

    /** Whether the screen shows this only for a few seconds. */
    public boolean passing() {
        return this == HELLO || this == NONE || this == DENY;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
