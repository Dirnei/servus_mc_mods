package io.github.dirnei.mc.servus.delicacies.stove;

import java.util.function.IntPredicate;

/**
 * Finds the straight row of loose Stove Tiles a stove forms from. Pure Java. Offsets count along the row from the
 * clicked tile at 0.
 */
public final class StoveRow {

    /** The fewest tiles a stove has. */
    public static final int MIN_LENGTH = 2;

    /** Why a row did not form, or that it did. */
    public enum Outcome {
        FORMED,
        TOO_SHORT,
        TOO_LONG
    }

    /** The row from offset {@code from} to {@code to}, both included, when {@link #outcome()} is FORMED. */
    public record Result(Outcome outcome, int from, int to) {

        public boolean formed() {
            return outcome == Outcome.FORMED;
        }

        public int length() {
            return to - from + 1;
        }
    }

    private StoveRow() {
    }

    /**
     * The row through offset 0 in which {@code looseTileAt} holds, with at least {@link #MIN_LENGTH} and at most
     * {@code maxLength} tiles. Looks at most {@code maxLength} tiles each way.
     */
    public static Result find(IntPredicate looseTileAt, int maxLength) {
        int from = 0;
        while (from > -maxLength && looseTileAt.test(from - 1)) {
            from--;
        }
        int to = 0;
        while (to < maxLength && looseTileAt.test(to + 1)) {
            to++;
        }
        int length = to - from + 1;
        if (length > maxLength) {
            return new Result(Outcome.TOO_LONG, from, to);
        }
        if (length < MIN_LENGTH) {
            return new Result(Outcome.TOO_SHORT, from, to);
        }
        return new Result(Outcome.FORMED, from, to);
    }
}
