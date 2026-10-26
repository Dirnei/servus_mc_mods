package io.github.dirnei.mc.servus.shuttles.terminal;

/**
 * Checks the numbers of the Storage Terminal's Rule tab before Save: Order
 * when below at least 0, Fill up to at least 1, and the first below the
 * second. Pure, no Minecraft imports.
 */
public final class RuleInput {

    /** Order when below is negative. */
    public static final String MIN_LOW = "rule_min_low";
    /** Fill up to is below 1. */
    public static final String MAX_LOW = "rule_max_low";
    /** Order when below is not below Fill up to. */
    public static final String ORDER = "rule_order";

    private RuleInput() {
    }

    /** The reason key Save is greyed out for, or null when the rule may be saved. */
    public static String refusal(int min, int max) {
        if (min < 0) {
            return MIN_LOW;
        }
        if (max < 1) {
            return MAX_LOW;
        }
        return min < max ? null : ORDER;
    }
}
