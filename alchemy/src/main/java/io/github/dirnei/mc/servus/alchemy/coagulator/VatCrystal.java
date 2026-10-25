package io.github.dirnei.mc.servus.alchemy.coagulator;

import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CrystalRamp;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** What has grown out of a Principle Vat: nothing, or the crystal an Athanor ritual grew in it. */
public enum VatCrystal implements StringRepresentable {
    NONE(CrystalRamp.of(0, 0, 0, 0, 0), 0),
    CITRINE(CrystalRamp.of(0xD29E00, 0xF0C000, 0xFFD92E, 0xFFE96A, 0xFFF6B8), 10),
    /** The citrine the Rubedo ritual turned orange. */
    RUBEDO(CrystalRamp.of(0xC4500A, 0xEA6E10, 0xFF8A24, 0xFFB055, 0xFFDCAA), 10);

    private final CrystalRamp ramp;
    private final int light;

    VatCrystal(CrystalRamp ramp, int light) {
        this.ramp = ramp;
        this.light = light;
    }

    /** The colours of the crystal from shadow to highlight. */
    public CrystalRamp ramp() {
        return ramp;
    }

    /** RGB of the crystal's glow and of the liquid it grows from: the middle of its ramp. */
    public int color() {
        return ramp.stop(2);
    }

    /** The light level the vat gives. */
    public int light() {
        return light;
    }

    public boolean grown() {
        return this != NONE;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
