package io.github.dirnei.mc.servus.foundation.filter;

/**
 * The wear of one filter: its cleaning {@code points} and how many of them are {@code used}.
 * Pure logic; the item side lives in {@link Filters}.
 */
public record FilterWear(int points, int used) {

    /** The outcome of {@link #spend}: the points actually spent, the new used value and whether nothing is left. */
    public record Spend(int spent, int used, boolean wornOut) {
    }

    /** The points still left, never below zero. */
    public int left() {
        return Math.max(0, points - used);
    }

    /** Spends up to {@code requested} points; a zero or negative request or a worn out filter spends nothing. */
    public Spend spend(int requested) {
        int spent = requested <= 0 ? 0 : Math.min(requested, left());
        int newUsed = used + spent;
        return new Spend(spent, newUsed, newUsed >= points);
    }

    /** The width of a durability bar of {@code maxWidth} for the points left; at least 1 while any point is left. */
    public int barWidth(int maxWidth) {
        int left = left();
        if (left <= 0 || points <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(maxWidth, Math.round((float) maxWidth * left / points)));
    }
}
