package io.github.dirnei.mc.servus.storage.site;

/**
 * The wake of one site: asleep, waiting to wake, awake while wanted or busy,
 * then a grace time before it releases its chunks.
 */
public final class WakeMachine {

    public enum State { ASLEEP, WAITING, AWAKE, GRACE }

    /** What the caller has to do after a tick. */
    public enum Action { NONE, LOAD, RELEASE }

    private State state = State.ASLEEP;
    private WakeAllowance.Reason reason = WakeAllowance.Reason.ALLOWED;
    private int graceLeft;

    public State state() {
        return state;
    }

    /** Why the site waits; {@code ALLOWED} while it does not. */
    public WakeAllowance.Reason reason() {
        return reason;
    }

    /** Ticks of grace left. */
    public int graceLeft() {
        return graceLeft;
    }

    /** Whether the site's chunks are loaded by this wake. */
    public boolean awake() {
        return state == State.AWAKE || state == State.GRACE;
    }

    /**
     * One tick.
     *
     * @param wanted     a request or a hold wants the site
     * @param busy       the site's own work runs
     * @param allowance  whether it may wake now
     * @param graceTicks how long it stays after the work is done
     */
    public Action tick(boolean wanted, boolean busy, WakeAllowance.Reason allowance, int graceTicks) {
        switch (state) {
            case ASLEEP, WAITING -> {
                if (!wanted) {
                    state = State.ASLEEP;
                    reason = WakeAllowance.Reason.ALLOWED;
                    return Action.NONE;
                }
                if (allowance != WakeAllowance.Reason.ALLOWED) {
                    state = State.WAITING;
                    reason = allowance;
                    return Action.NONE;
                }
                state = State.AWAKE;
                reason = WakeAllowance.Reason.ALLOWED;
                return Action.LOAD;
            }
            case AWAKE, GRACE -> {
                if (allowance.hardLimit()) {
                    return sleep(wanted ? allowance : WakeAllowance.Reason.ALLOWED);
                }
                if (wanted || busy) {
                    state = State.AWAKE;
                    return Action.NONE;
                }
                if (state == State.AWAKE) {
                    state = State.GRACE;
                    graceLeft = graceTicks;
                }
                if (--graceLeft < 0) {
                    return sleep(WakeAllowance.Reason.ALLOWED);
                }
                return Action.NONE;
            }
            default -> {
                return Action.NONE;
            }
        }
    }

    private Action sleep(WakeAllowance.Reason why) {
        graceLeft = 0;
        reason = why;
        state = why == WakeAllowance.Reason.ALLOWED ? State.ASLEEP : State.WAITING;
        return Action.RELEASE;
    }
}
