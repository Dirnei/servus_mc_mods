package io.github.dirnei.mc.servus.foundation.side;

/** The layers of a side configuration: what kind of resource a side setting is about. */
public enum SideLayer {
    ITEMS,
    FLUIDS;

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The layer after this one, wrapping around. */
    public SideLayer next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** The layer with this name, or Items for anything unknown. */
    public static SideLayer byName(String name) {
        for (SideLayer layer : values()) {
            if (layer.name().equalsIgnoreCase(name)) {
                return layer;
            }
        }
        return ITEMS;
    }
}
