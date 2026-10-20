package io.github.dirnei.mc.servus.brewery.beer;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** What the top of a Fermenting Vat shows. */
public enum VatContent implements StringRepresentable {
    EMPTY,
    WORT,
    FERMENTING,
    BEER;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
