package io.github.dirnei.mc.servus.shuttles.order;

/**
 * The threshold rules of a stock rule, shared by the terminal, the rule
 * payload and the commands: a rule needs a Make pair, a cap or an export, a
 * Make pair orders below its minimum up to a higher maximum, a cap is not
 * below the maximum and an export is above it. Pure, no Minecraft imports.
 */
public final class RuleThresholds {

    public static final String OK = "ok";
    /** Neither a Make pair, nor a cap, nor an export. */
    public static final String EMPTY = "empty";
    /** The minimum is not below the maximum, or the maximum is below 1. */
    public static final String MAKE_ORDER = "make_order";
    /** The cap is below the maximum. */
    public static final String CAP_BELOW_MAX = "cap_below_max";
    /** The export threshold is not above the maximum. */
    public static final String EXPORT_NOT_ABOVE = "export_not_above";

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
        return check(min, max, cap, -1);
    }

    /**
     * Checks a candidate rule with an export threshold.
     *
     * @param export the export threshold, below 0 for none
     * @see #check(int, int, int)
     */
    public static String check(int min, int max, int cap, int export) {
        boolean make = min >= 0 || max >= 0;
        boolean capped = cap > 0;
        boolean exported = export >= 0;
        if (!make && !capped && !exported) {
            return EMPTY;
        }
        if (make && (min < 0 || max < 1 || min >= max)) {
            return MAKE_ORDER;
        }
        if (make && capped && cap < max) {
            return CAP_BELOW_MAX;
        }
        if (exported && !ExportPlan.validExport(make ? max : -1, export)) {
            return EXPORT_NOT_ABOVE;
        }
        return OK;
    }
}
