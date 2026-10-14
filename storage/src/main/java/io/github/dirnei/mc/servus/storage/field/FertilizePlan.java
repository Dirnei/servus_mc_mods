package io.github.dirnei.mc.servus.storage.field;

/**
 * When a field needs a Spreader and how much fertilizer it takes from the
 * Field Head's buffer, and the reserve supply runs keep in storage. Pure Java.
 */
public final class FertilizePlan {

    /** The largest reserve a head keeps; container data travels as a short. */
    public static final int MAX_RESERVE = 9999;

    private FertilizePlan() {
    }

    /**
     * How many fertilizer items a Spreader takes before it drives out: what
     * the plots need, but never the stock at or below the reserve and never
     * more than the cargo has room for.
     *
     * @param plots   plots that need a dose
     * @param doses   doses per plot in one round
     * @param stock   fertilizer items it may take from
     * @param reserve items it must leave
     * @param room    items the cargo can still take
     */
    public static int toFetch(int plots, int doses, long stock, int reserve, int room) {
        long wanted = Math.min((long) Math.max(0, plots) * Math.max(0, doses), Math.min(stock - Math.max(0, reserve), room));
        return (int) Math.max(0, wanted);
    }

    /**
     * How many plots need a dose by the mode.
     *
     * @param mode          the head's fertilize mode
     * @param unfedGrowing  plots planted since their last dose that are not ripe
     * @param growing       planted plots that are not ripe
     * @param ripe          ripe plots
     * @param planted       planted plots, ripe ones included
     * @param percent       the head's fertilize threshold in percent
     */
    public static int targets(FertilizeMode mode, int unfedGrowing, int growing, int ripe, int planted, int percent) {
        return switch (mode) {
            case OFF -> 0;
            case AFTER_PLANTING -> Math.max(0, unfedGrowing);
            case UNTIL_RIPE -> belowShare(ripe, planted, percent) ? Math.max(0, growing) : 0;
        };
    }

    /** Whether the ripe share of the planted plots is below the threshold; nothing planted counts as below. */
    public static boolean belowShare(int ripe, int planted, int percent) {
        return planted <= 0 || (long) ripe * 100 < (long) percent * planted;
    }

    /** Whether at least the interval passed since the last call; a field that never called may call. */
    public static boolean intervalPassed(long now, long lastCall, int interval) {
        return lastCall < 0 || now - lastCall >= interval;
    }

    /** Whether the field calls a Spreader now: plots need a dose, stock above the reserve and the interval passed. */
    public static boolean due(int targets, long stock, int reserve, long now, long lastCall, int interval) {
        return targets > 0 && stock > Math.max(0, reserve) && intervalPassed(now, lastCall, interval);
    }

    /** The reserve kept between 0 and {@link #MAX_RESERVE}. */
    public static int clampReserve(long reserve) {
        return (int) Math.max(0, Math.min(MAX_RESERVE, reserve));
    }
}
