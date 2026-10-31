package io.github.dirnei.mc.servus.foundation.data;

/**
 * Decides one upkeep check of a member: credits arrivals counted from the
 * member's own count against its open request, ends the request when complete,
 * when its supplier ticket closed or after its timeout, and says how much to ask for.
 */
public final class UpkeepBook {

    /** What the supplier says about the open request's ticket. */
    public enum TicketState {
        OPEN,
        CLOSED,
        /** The supplier's block is not loaded, so it can neither be asked nor told to cancel. */
        UNREACHABLE
    }

    /**
     * The outcome of a check.
     *
     * @param keep   the open request to keep, null when it ended or there was none
     * @param cancel whether the ended request's ticket must be cancelled at its supplier
     * @param ask    how many to ask for now, 0 for nothing
     */
    public record Step(UpkeepRequest keep, boolean cancel, int ask) {
    }

    private UpkeepBook() {
    }

    /**
     * One check of a member holding {@code count} that keeps {@code level}
     * and waits for {@code batch} missing before it asks again.
     *
     * @param open   the member's open request, null for none
     * @param ticket the state of the open request's ticket, ignored without one
     */
    public static Step step(UpkeepRequest open, int count, int level, int batch, TicketState ticket, long now) {
        boolean cancel = false;
        if (open != null) {
            UpkeepRequest counted = open.counted(count);
            boolean ended = counted.remaining() <= 0 || ticket == TicketState.CLOSED;
            if (!ended && now > counted.until()) {
                if (ticket == TicketState.UNREACHABLE) {
                    return new Step(counted, false, 0);
                }
                ended = true;
                cancel = true;
            }
            if (!ended) {
                return new Step(counted, false, 0);
            }
        }
        return new Step(null, cancel, missing(count, level, batch));
    }

    /** How many to ask for: everything missing, once at least a batch (at most the level) is missing or none is held. */
    public static int missing(int count, int level, int batch) {
        int missing = level - count;
        if (missing <= 0) {
            return 0;
        }
        int effective = Math.max(1, Math.min(batch, level));
        return count <= 0 || missing >= effective ? missing : 0;
    }
}
