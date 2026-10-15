package io.github.dirnei.mc.servus.shuttles.gametest;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTestHelper;

/** Waiting for a condition in GameTests. */
public final class Waits {

    private Waits() {
    }

    /**
     * Polls every tick until {@code condition} holds, then runs {@code then}; fails after {@code maxTicks}.
     * Each poll is a fresh lambda: the GameTest scheduler keys tasks by instance and would drop a re-added one.
     */
    public static void when(GameTestHelper helper, BooleanSupplier condition, int maxTicks, Supplier<String> failure, Runnable then) {
        int[] left = {maxTicks};
        Runnable[] poll = new Runnable[1];
        poll[0] = () -> {
            if (condition.getAsBoolean()) {
                then.run();
            } else if (--left[0] <= 0) {
                helper.fail(failure.get());
            } else {
                helper.runAfterDelay(1, () -> poll[0].run());
            }
        };
        helper.runAfterDelay(1, () -> poll[0].run());
    }
}
