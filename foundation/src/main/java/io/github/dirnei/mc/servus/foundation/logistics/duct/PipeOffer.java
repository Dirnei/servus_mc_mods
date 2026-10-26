package io.github.dirnei.mc.servus.foundation.logistics.duct;

/**
 * How many items an Item Pipe offers a receiving side that answers offers:
 * what its pulling source can give after the keep amount, at most a stack,
 * and no more than the receiving side's fill up to amount still allows.
 * Pure Java.
 */
public final class PipeOffer {

    /** The receiving side has no fill up to amount. */
    public static final int NO_ALLOWANCE = -1;

    private PipeOffer() {
    }

    /**
     * @param movable          how many of the item the source gives after its keep amount
     * @param stackSize        the item's stack size
     * @param receiveAllowance how many more the receiving side takes by its fill up to amount, or {@link #NO_ALLOWANCE}
     */
    public static int amount(long movable, int stackSize, int receiveAllowance) {
        long amount = Math.min(movable, Math.max(1, stackSize));
        if (receiveAllowance != NO_ALLOWANCE) {
            amount = Math.min(amount, receiveAllowance);
        }
        return (int) Math.max(0, amount);
    }
}
