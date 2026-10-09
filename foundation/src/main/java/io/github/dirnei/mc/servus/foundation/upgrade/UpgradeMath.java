package io.github.dirnei.mc.servus.foundation.upgrade;

import java.util.Collection;

/** How upgrades combine. Pure, no Minecraft imports. */
public final class UpgradeMath {

    private UpgradeMath() {
    }

    /**
     * The factor a block's rate is multiplied with: the product of the
     * multipliers of the upgrades it holds, 1 without upgrades. Multipliers
     * below zero count as zero.
     */
    public static double multiplier(Collection<Double> multipliers) {
        double product = 1;
        for (double value : multipliers) {
            product *= Math.max(0, value);
        }
        return product;
    }
}
