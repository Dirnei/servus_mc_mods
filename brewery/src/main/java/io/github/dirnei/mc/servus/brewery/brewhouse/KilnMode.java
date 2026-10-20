package io.github.dirnei.mc.servus.brewery.brewhouse;

/** How long a kiln tray keeps its malt on the fire. Pure Java. */
public enum KilnMode {
    /** Dried for the recipe's time: pale malt. */
    PALE,
    /** Kept on the fire for the recipe's dark time as well: dark malt. */
    DARK;

    /** The other mode, for a carrot click. */
    public KilnMode next() {
        return this == PALE ? DARK : PALE;
    }
}
