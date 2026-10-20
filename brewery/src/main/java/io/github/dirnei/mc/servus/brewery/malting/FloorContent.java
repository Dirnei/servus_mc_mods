package io.github.dirnei.mc.servus.brewery.malting;

import net.minecraft.util.StringRepresentable;

/** What lies on a Malting Floor, as its block state shows it. */
public enum FloorContent implements StringRepresentable {
    EMPTY("empty"),
    SOAKED("soaked"),
    GREEN("green");

    private final String name;

    FloorContent(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
