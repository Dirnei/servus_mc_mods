package io.github.dirnei.mc.servus.shuttles.gantry;

import net.minecraft.util.StringRepresentable;

/** The block state value of a gantry part; each names one {@link GantryLayout.Part}. */
public enum GantryPart implements StringRepresentable {
    LEFT_LOW("left_low"), LEFT_HIGH("left_high"), RIGHT_LOW("right_low"), RIGHT_HIGH("right_high"), BEAM("beam");

    private final String name;

    GantryPart(String name) {
        this.name = name;
    }

    public GantryLayout.Part layout() {
        return GantryLayout.Part.valueOf(name());
    }

    public static GantryPart of(GantryLayout.Part part) {
        return valueOf(part.name());
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
