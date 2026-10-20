package io.github.dirnei.mc.servus.delicacies.crock;

/** How many times a recipe fits into what a crock holds. */
public final class CrockBatch {

    private CrockBatch() {
    }

    /**
     * The number of full batches: each ingredient {@code i} has {@code have[i]} and needs {@code need[i]} per batch,
     * the fluid has {@code fluidHave} and needs {@code fluidNeed} (no fluid when 0), and the output takes
     * {@code outputRoom} more items of {@code resultCount} per batch.
     */
    public static int count(int[] have, int[] need, int fluidHave, int fluidNeed, int outputRoom, int resultCount) {
        int batches = resultCount <= 0 ? 0 : outputRoom / resultCount;
        for (int i = 0; i < need.length; i++) {
            if (need[i] > 0) {
                batches = Math.min(batches, have[i] / need[i]);
            }
        }
        if (fluidNeed > 0) {
            batches = Math.min(batches, fluidHave / fluidNeed);
        }
        return Math.max(0, batches);
    }
}
