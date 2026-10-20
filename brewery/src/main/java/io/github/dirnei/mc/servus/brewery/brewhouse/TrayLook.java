package io.github.dirnei.mc.servus.brewery.brewhouse;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** What a kiln tray shows: nothing, malt still drying, or the pale or dark malt of the last batch. */
public enum TrayLook implements StringRepresentable {
    EMPTY,
    GREEN,
    PALE,
    DARK;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
