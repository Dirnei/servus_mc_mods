package io.github.dirnei.mc.servus.foundation.data.packet;

/**
 * The bandwidth of one data duct turned into a per tick allowance, with the
 * same fractional carry as {@link RateBudget}. A simplex duct holds one budget
 * both directions share; a duplex duct holds one per direction. A new budget
 * starts full (one second's worth), as an idle duct would be. It also counts
 * the items that passed in each of the last {@link #WINDOW} ticks, so the
 * load over the last second can be shown.
 */
public final class DuctBudget {

    /** Ticks of the load window: one second. */
    public static final int WINDOW = RateBudget.TICKS_PER_SECOND;

    /** The highest load level, a duct at or near its full bandwidth. */
    public static final int FULL = 4;

    private final RateBudget forward;
    private final RateBudget backward;
    private boolean duplex;
    private final int[][] used = new int[2][WINDOW];
    private int slot;

    public DuctBudget(double perSecond, boolean duplex) {
        this.duplex = duplex;
        this.forward = new RateBudget(perSecond);
        this.backward = new RateBudget(perSecond);
        forward.fill();
        backward.fill();
    }

    /** Items per second, per direction for duplex, shared for simplex. */
    public double rate() {
        return forward.rate();
    }

    public boolean duplex() {
        return duplex;
    }

    /** Follows a tier change or a datapack reload. */
    public void setRate(double perSecond, boolean duplex) {
        if (forward.rate() != perSecond) {
            forward.setRate(perSecond);
            backward.setRate(perSecond);
        }
        this.duplex = duplex;
    }

    /** Starts the next tick: refills the budgets and forgets what passed one second ago. */
    public void tick() {
        forward.tick();
        backward.tick();
        slot = (slot + 1) % WINDOW;
        used[0][slot] = 0;
        used[1][slot] = 0;
    }

    /** How many items may pass in that direction right now. */
    public int allowance(boolean forwards) {
        return budget(forwards).allowance();
    }

    /** Records that {@code items} passed in that direction. */
    public void consume(boolean forwards, int items) {
        if (items <= 0) {
            return;
        }
        budget(forwards).consume(items);
        used[forwards ? 0 : 1][slot] += items;
    }

    /** Items that passed in that direction during the last second. */
    public int used(boolean forwards) {
        int sum = 0;
        for (int value : used[forwards ? 0 : 1]) {
            sum += value;
        }
        return sum;
    }

    /**
     * The load over the last second in items per second, comparable with
     * {@link #rate()}: both directions together for simplex, the busier one
     * for duplex.
     */
    public int load() {
        return duplex ? Math.max(used(true), used(false)) : used(true) + used(false);
    }

    /** Whether nothing passed during the last second. */
    public boolean idle() {
        return used(true) == 0 && used(false) == 0;
    }

    /**
     * A load bucketed for display: 0 idle, 1 up to a quarter, 2 up to half,
     * 3 below 90 percent, {@link #FULL} from 90 percent of the bandwidth.
     */
    public static int level(int load, double bandwidth) {
        if (load <= 0) {
            return 0;
        }
        double share = bandwidth <= 0 ? 1 : load / bandwidth;
        if (share >= 0.9) {
            return FULL;
        }
        if (share >= 0.5) {
            return 3;
        }
        return share >= 0.25 ? 2 : 1;
    }

    private RateBudget budget(boolean forwards) {
        return duplex && !forwards ? backward : forward;
    }
}
