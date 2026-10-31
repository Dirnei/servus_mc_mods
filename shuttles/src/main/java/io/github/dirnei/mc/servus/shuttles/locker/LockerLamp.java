package io.github.dirnei.mc.servus.shuttles.locker;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** A box lamp of a Parcel Locker: dark while free, orange while reserved or going back, green while a parcel is ready. */
public enum LockerLamp implements StringRepresentable {
    OFF,
    RESERVED,
    READY;

    /** The lamp a box of an order in that state shows. */
    public static LockerLamp of(LockerReturns.State state) {
        return state == LockerReturns.State.READY ? READY : RESERVED;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
