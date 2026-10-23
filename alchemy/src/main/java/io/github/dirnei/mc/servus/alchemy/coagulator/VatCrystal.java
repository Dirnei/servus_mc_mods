package io.github.dirnei.mc.servus.alchemy.coagulator;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** What has grown out of a Principle Vat: nothing, or the crystal an Athanor ritual grew in it. */
public enum VatCrystal implements StringRepresentable {
    NONE(0, 0, 0),
    CITRINE(0xE8B030, 0xFFB000, 10),
    /** The citrine the Rubedo ritual turned orange. */
    RUBEDO(0xF07A18, 0xFF6A00, 10);

    private final int body;
    private final int color;
    private final int light;

    VatCrystal(int body, int color, int light) {
        this.body = body;
        this.color = color;
        this.light = light;
    }

    /** RGB of the crystal's body. */
    public int body() {
        return body;
    }

    /** RGB of the crystal's glow and of the liquid it grows from. */
    public int color() {
        return color;
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
