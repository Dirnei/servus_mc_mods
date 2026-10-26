package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.order.RuleThresholds;

/**
 * The numbers of the Storage Terminal's Rule tab as a rule: Fill up to and the
 * cap at 0 are off, Export above below 0 is off, and with Fill up to off and
 * Order when below at 0 the rule has no Make pair. Pure, no Minecraft imports.
 *
 * @param min    the Make minimum, -1 without a Make pair
 * @param max    the Make maximum, -1 without a Make pair
 * @param cap    refuse at Inbound above this, 0 for none
 * @param export export above this, -1 for none
 */
public record RuleInput(int min, int max, int cap, int export) {

    /**
     * The fields as typed: Order when below, Fill up to (0 is off), Refuse at
     * Inbound above (0 is off) and Export above (below 0 is off).
     */
    public static RuleInput of(int orderBelow, int fillUpTo, int refuseAbove, int exportAbove) {
        boolean make = fillUpTo > 0 || orderBelow > 0;
        return new RuleInput(make ? orderBelow : -1, make ? fillUpTo : -1, Math.max(0, refuseAbove), Math.max(-1, exportAbove));
    }

    public boolean hasMake() {
        return max >= 0;
    }

    /** The lang key suffix Save is greyed out for, or null when the rule may be saved. */
    public String refusal() {
        String check = RuleThresholds.check(min, max, cap, export);
        return check.equals(RuleThresholds.OK) ? null : "rule_" + check;
    }
}
