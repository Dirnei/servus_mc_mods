package io.github.dirnei.mc.servus.alchemy.coagulator;

/** How many seeds the Coagulator's seed slot takes: up to a stack of one kind, never two kinds at once. */
public final class SeedSlot {

    private SeedSlot() {
    }

    /**
     * How many more of an offered seed fit.
     *
     * @param held         how many seeds the slot holds now, 0 when it is empty
     * @param sameKind     whether the offered seed is the same item as the one held
     * @param maxStackSize the offered item's own stack size
     */
    public static int room(int held, boolean sameKind, int maxStackSize) {
        if (held <= 0) {
            return maxStackSize;
        }
        return sameKind ? Math.max(0, maxStackSize - held) : 0;
    }
}
