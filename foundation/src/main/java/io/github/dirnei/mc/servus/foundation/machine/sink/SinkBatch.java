package io.github.dirnei.mc.servus.foundation.machine.sink;

/**
 * Plans one batch of a Sink: how much to take from each slot or tank, walking
 * from index 0 until the budget is spent. Serves item counts and fluid amounts
 * alike. Pure Java.
 */
public final class SinkBatch {

    private SinkBatch() {
    }

    /**
     * How much one batch takes from each index.
     *
     * @param amounts what each index holds
     * @param budget  how much the batch may take in total, or {@link SinkRates#UNLIMITED} for everything
     * @return an array as long as {@code amounts}; indices past the budget take 0
     */
    public static int[] plan(int[] amounts, double budget) {
        int[] taken = new int[amounts.length];
        double left = Math.max(0, budget);
        for (int index = 0; index < amounts.length && left >= 1; index++) {
            int amount = Math.max(0, amounts[index]);
            int take = left >= amount ? amount : (int) left;
            taken[index] = take;
            left -= take;
        }
        return taken;
    }
}
