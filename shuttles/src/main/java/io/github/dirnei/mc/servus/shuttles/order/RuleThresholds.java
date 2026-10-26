package io.github.dirnei.mc.servus.shuttles.order;

/**
 * The threshold rules of a stock rule, shared by the terminal, the rule
 * payload and the commands: a rule needs a Make pair or a cap, a Make pair
 * orders below its minimum up to a higher maximum, and a cap is not below the
 * maximum. Pure, no Minecraft imports.
 */
public final class RuleThresholds {

    public static final String OK = "ok";
    /** Neither a Make pair nor a cap. */
    public static final String EMPTY = "empty";
    /** The minimum is not below the maximum, or the maximum is below 1. */
    public static final String MAKE_ORDER = "make_order";
    /** The cap is below the maximum. */
    public static final String CAP_BELOW_MAX = "cap_below_max";

    private RuleThresholds() {
    }

    /**
     * Checks a candidate rule.
     *
     * @param min the Make minimum, -1 together with {@code max} for no Make pair
     * @param max the Make maximum, -1 together with {@code min} for no Make pair
     * @param cap the cap, 0 or less for none
     * @return {@link #OK} or the reason the rule breaks the threshold rules
     */
    public static String check(int min, int max, int cap) {
        boolean make = min >= 0 || max >= 0;
        boolean capped = cap > 0;
        if (!make && !capped) {
            return EMPTY;
        }
        if (make && (min < 0 || max < 1 || min >= max)) {
            return MAKE_ORDER;
        }
        if (make && capped && cap < max) {
            return CAP_BELOW_MAX;
        }
        return OK;
    }
}
