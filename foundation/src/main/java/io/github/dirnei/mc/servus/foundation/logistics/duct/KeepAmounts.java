package io.github.dirnei.mc.servus.foundation.logistics.duct;

/**
 * How much a duct side may move of one filter entry with an amount: a giving
 * side keeps the amount in its block, a receiving side fills its block up to
 * it. An amount of 0 or less means none, and the whole offer passes. Pure Java.
 */
public final class KeepAmounts {

    private KeepAmounts() {
    }

    /** How much of {@code offered} may leave a block that holds {@code held} and keeps {@code keep}. */
    public static int giveAllowance(long held, int keep, int offered) {
        return keep <= 0 ? offered : clamp(held - keep, offered);
    }

    /** How much of {@code offered} a block that holds {@code held} may receive to reach {@code upTo}. */
    public static int receiveAllowance(long held, int upTo, int offered) {
        return upTo <= 0 ? offered : clamp(upTo - held, offered);
    }

    private static int clamp(long room, int offered) {
        return (int) Math.max(0, Math.min(offered, room));
    }
}
