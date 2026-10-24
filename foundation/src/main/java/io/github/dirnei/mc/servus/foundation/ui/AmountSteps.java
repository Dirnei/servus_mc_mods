package io.github.dirnei.mc.servus.foundation.ui;

/**
 * The step buttons of an amount field, such as the {@code NumberStepper}. Stack
 * steps snap to full stacks first: +stack from 1 gives 64, not 65. Amounts
 * stay between 1 and {@link #MAX}. Pure, no Minecraft imports.
 */
public final class AmountSteps {

    public static final int MIN = 1;
    public static final int MAX = 99999;

    /** A step of {@code STACK} or {@code -STACK} in {@link #step} means a full stack up or down. */
    public static final int STACK = Integer.MAX_VALUE;

    private AmountSteps() {
    }

    /**
     * The amount after one step between {@code min} and {@code max}: a plain
     * step adds itself, {@link #STACK} snaps to the next full stack up and
     * {@code -STACK} to the one below.
     */
    public static int step(int amount, int step, int stack, int min, int max) {
        int size = Math.max(1, stack);
        long next;
        if (step == STACK) {
            next = ((long) Math.max(0, amount) / size + 1) * size;
        } else if (step == -STACK) {
            next = ((long) amount - 1) / size * size;
        } else {
            next = (long) amount + step;
        }
        return (int) Math.max(min, Math.min(max, next));
    }

    /** The amount plus {@code by} (negative to subtract), kept between 1 and 99999. */
    public static int add(int amount, int by) {
        return clamp((long) amount + by);
    }

    /** The smallest multiple of {@code stack} above {@code amount}: 1 to 64, 64 to 128, 70 to 128. */
    public static int plusStack(int amount, int stack) {
        int size = Math.max(1, stack);
        long next = ((long) Math.max(0, amount) / size + 1) * size;
        return clamp(next);
    }

    /** The largest multiple of {@code stack} below {@code amount}, at least 1: 70 to 64, 128 to 64, 64 to 1. */
    public static int minusStack(int amount, int stack) {
        int size = Math.max(1, stack);
        long previous = ((long) amount - 1) / size * size;
        return clamp(previous);
    }

    private static int clamp(long value) {
        return (int) Math.max(MIN, Math.min(MAX, value));
    }
}
