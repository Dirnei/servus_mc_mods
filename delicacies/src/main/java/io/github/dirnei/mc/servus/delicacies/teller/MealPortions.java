package io.github.dirnei.mc.servus.delicacies.teller;

/** How a placed meal is eaten: it holds {@link #FULL} portions and each empty hand click eats one. */
public final class MealPortions {

    /** Portions on a fresh meal. */
    public static final int FULL = 4;

    private MealPortions() {
    }

    /** Whether eating one of {@code left} portions finishes the meal. */
    public static boolean isLast(int left) {
        check(left);
        return left == 1;
    }

    /** The portions left after eating one of {@code left}; the last portion has no such state. */
    public static int afterEating(int left) {
        if (isLast(left)) {
            throw new IllegalArgumentException("the last portion leaves an empty plate");
        }
        return left - 1;
    }

    static void check(int left) {
        if (left < 1 || left > FULL) {
            throw new IllegalArgumentException("portions " + left + " is outside 1 to " + FULL);
        }
    }
}
