package io.github.dirnei.mc.servus.lib.flow;

/**
 * Shares a budget among receivers by water filling: equal shares per round,
 * receivers whose demand is met drop out and the rest keep filling. Used by
 * the energy and fluid distributors. Pure Java.
 */
public final class WaterFilling {

    private WaterFilling() {
    }

    /**
     * @param demand most each receiver takes, zero or negative for none
     * @param budget total to share
     * @return the share of each receiver, never above its demand, summing to at most the budget
     */
    public static int[] allocate(int[] demand, int budget) {
        int[] allocation = new int[demand.length];
        int active = 0;
        for (int d : demand) {
            if (d > 0) {
                active++;
            }
        }
        while (budget > 0 && active > 0) {
            int share = Math.max(1, budget / active);
            boolean progressed = false;
            for (int i = 0; i < demand.length && budget > 0; i++) {
                int remaining = demand[i] - allocation[i];
                if (remaining <= 0) {
                    continue;
                }
                int give = Math.min(Math.min(share, remaining), budget);
                allocation[i] += give;
                budget -= give;
                progressed |= give > 0;
                if (allocation[i] == demand[i]) {
                    active--;
                }
            }
            if (!progressed) {
                break;
            }
        }
        return allocation;
    }
}
