package io.github.dirnei.mc.servus.shuttles.terminal;

/**
 * Sorts the warehouses found among the members of a data line into none, one
 * or a conflict. Warehouses compare by identity, so one warehouse seen through
 * its controller and several Packing Stations counts once. Pure Java.
 */
public final class LineClassification {

    /** How many distinct warehouses a line reaches. */
    public enum Kind {
        NONE,
        ONE,
        CONFLICT
    }

    /**
     * @param warehouse the only warehouse while the kind is {@link Kind#ONE}, else null
     */
    public record Result<W>(Kind kind, W warehouse) {
    }

    private LineClassification() {
    }

    public static <W> Result<W> classify(Iterable<W> found) {
        W only = null;
        for (W warehouse : found) {
            if (only == null) {
                only = warehouse;
            } else if (only != warehouse) {
                return new Result<>(Kind.CONFLICT, null);
            }
        }
        return only == null ? new Result<>(Kind.NONE, null) : new Result<>(Kind.ONE, only);
    }
}
