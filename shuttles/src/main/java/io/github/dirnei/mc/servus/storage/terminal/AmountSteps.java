package io.github.dirnei.mc.servus.storage.terminal;

/**
 * The step buttons of the terminal's amount prompt and craft dialog. Stack
 * steps snap to full stacks first: +stack from 1 gives 64, not 65. Amounts
 * stay between 1 and {@link #MAX}. Pure, no Minecraft imports.
 */
public final class AmountSteps {

    public static final int MIN = 1;
    public static final int MAX = 99999;

    private AmountSteps() {
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
