package io.github.dirnei.mc.servus.foundation.logistics.duct;

/** Which side the endpoint GUI of a duct opens on. Sides are direction ordinals: down, up, north, south, west, east. Pure Java. */
public final class DuctSides {

    public static final int NONE = -1;

    private DuctSides() {
    }

    /**
     * The clicked side when it is selectable, else the first selectable side
     * in direction order, else {@link #NONE}.
     *
     * @param selectable per side whether the GUI may show it
     * @param clicked    the clicked arm's side, or {@link #NONE} for the centre
     */
    public static int startSide(boolean[] selectable, int clicked) {
        if (clicked >= 0 && clicked < selectable.length && selectable[clicked]) {
            return clicked;
        }
        for (int side = 0; side < selectable.length; side++) {
            if (selectable[side]) {
                return side;
            }
        }
        return NONE;
    }
}
