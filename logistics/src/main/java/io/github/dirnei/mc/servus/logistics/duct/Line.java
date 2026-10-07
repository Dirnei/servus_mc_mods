package io.github.dirnei.mc.servus.logistics.duct;

/** What a utility duct can carry: the Item Pipe, the Power Cable or the Fluid Pipe. Each line is placed as its own item. */
public enum Line {
    ITEM("item"),
    ENERGY("energy"),
    FLUID("fluid");

    public static final Line[] VALUES = values();

    private final String id;

    Line(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Line byId(String id) {
        for (Line line : VALUES) {
            if (line.id.equals(id)) {
                return line;
            }
        }
        return null;
    }

    public static Line byOrdinal(int ordinal) {
        return VALUES[Math.floorMod(ordinal, VALUES.length)];
    }
}
