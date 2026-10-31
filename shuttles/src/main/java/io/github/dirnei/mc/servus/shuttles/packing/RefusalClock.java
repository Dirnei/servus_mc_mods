package io.github.dirnei.mc.servus.shuttles.packing;

/**
 * Decides whether a Packing Station gives up a shipment it cannot deliver. A
 * shipment cut from its target, or refused by a machine target, ends once that
 * lasted without a break for longer than the timeout; a removed target ends it
 * at once; an unloaded target and a player refusing (a full terminal Output
 * slot) never end it.
 */
public final class RefusalClock {

    /** What the station saw of a shipment's target this time. */
    public enum TargetState {
        /** The target's chunk is not loaded. */
        UNLOADED,
        /** No block at the loaded target takes items any more. */
        REMOVED,
        /** The station's data line does not reach the target. */
        CUT,
        /** The target refused the offer or the move. */
        REFUSING,
        /** The target granted room or took items. */
        TAKING
    }

    /**
     * The outcome of a judgement.
     *
     * @param end          whether the shipment ends short now
     * @param refusedSince game time of the first cut or refusal in a row, -1 for none
     */
    public record Verdict(boolean end, long refusedSince) {
    }

    private RefusalClock() {
    }

    /**
     * Judges one look at a shipment's target.
     *
     * @param playerTarget whether the target is a player's terminal, whose refusal never times out
     * @param refusedSince the clock so far, -1 when it does not run
     */
    public static Verdict judge(TargetState state, boolean playerTarget, long refusedSince, long now, int timeout) {
        return switch (state) {
            case UNLOADED, TAKING -> new Verdict(false, -1);
            case REMOVED -> new Verdict(true, refusedSince);
            case REFUSING -> playerTarget ? new Verdict(false, -1) : cut(refusedSince, now, timeout);
            case CUT -> cut(refusedSince, now, timeout);
        };
    }

    private static Verdict cut(long refusedSince, long now, int timeout) {
        long since = refusedSince < 0 ? now : refusedSince;
        return new Verdict(now - since > timeout, since);
    }
}
