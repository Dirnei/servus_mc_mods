package io.github.dirnei.mc.servus.manufacturing.alchemy;

/**
 * Salt that leaves the Calciner as Ash: whole Ash from mB, and the mB that Ash takes away.
 * A remainder smaller than one Ash stays behind. Pure Java.
 */
public final class AshCount {

    private AshCount() {
    }

    /** Whole Ash in {@code salt} mB when one Ash is worth {@code saltPerAsh} mB. */
    public static int ash(int salt, int saltPerAsh) {
        if (salt <= 0 || saltPerAsh <= 0) {
            return 0;
        }
        return salt / saltPerAsh;
    }

    /** The mB taking {@code ash} Ash removes, never more than whole Ash in {@code salt}. */
    public static int saltFor(int ash, int salt, int saltPerAsh) {
        return Math.max(0, Math.min(ash, ash(salt, saltPerAsh))) * Math.max(0, saltPerAsh);
    }
}
