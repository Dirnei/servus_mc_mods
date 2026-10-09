package io.github.dirnei.mc.servus.foundation.data.packet;

/** How many items a delivery may move in one tick. Pure, no Minecraft imports. */
public final class TransferAmount {

    /** The allowance of a member that is no gate. */
    public static final int NO_GATE = Integer.MAX_VALUE;

    private TransferAmount() {
    }

    /**
     * The items to move this tick: what is left of the delivery, bounded by what
     * the source can give, what the target takes and both gate allowances. Pass
     * {@link #NO_GATE} for a bound that does not apply.
     */
    public static int of(int left, int sourceHas, int targetTakes, int sourceGate, int targetGate) {
        int amount = Math.min(left, Math.min(sourceHas, targetTakes));
        amount = Math.min(amount, Math.min(sourceGate, targetGate));
        return Math.max(0, amount);
    }

    /** The bound the gates alone set this tick, before the source and target are asked. */
    public static int gates(int left, int sourceGate, int targetGate) {
        return of(left, NO_GATE, NO_GATE, sourceGate, targetGate);
    }
}
