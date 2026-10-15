package io.github.dirnei.mc.servus.shuttles.field;

/**
 * When a Field Head's supply buffer calls a supply run and how much one run
 * brings of each part. A level of 0 turns a part off. Pure Java.
 */
public final class SupplyPlan {

    private SupplyPlan() {
    }

    /**
     * One part of the buffer as the warehouse sees it.
     *
     * @param held    items the part holds
     * @param level   items the part is filled to; 0 turns it off
     * @param stock   items of the part's item the warehouse holds
     * @param reserve items the warehouse keeps; a supply run never takes them
     */
    public record Part(int held, int level, long stock, int reserve) {

        /** Whether the part calls a supply run: it is low and the warehouse holds more than the reserve. */
        public boolean due() {
            return low(held, level) && stock > Math.max(0, reserve);
        }
    }

    /** Whether a part holds less than half its level. */
    public static boolean low(int held, int level) {
        return level > 0 && (long) Math.max(0, held) * 2 < level;
    }

    /**
     * How many items a supply run brings for one part: what it is short of its
     * level, never the stock at or below the reserve and never more than the
     * cargo has room for.
     *
     * @param held    items the part holds
     * @param level   items the part is filled to
     * @param stock   items the warehouse holds
     * @param reserve items the warehouse keeps
     * @param room    items the cargo can still take
     */
    public static int toBring(int held, int level, long stock, int reserve, int room) {
        if (level <= 0) {
            return 0;
        }
        long wanted = Math.min((long) level - Math.max(0, held), Math.min(stock - Math.max(0, reserve), room));
        return (int) Math.max(0, wanted);
    }

    /** Whether any of the parts calls a supply run. */
    public static boolean due(Part... parts) {
        for (Part part : parts) {
            if (part.due()) {
                return true;
            }
        }
        return false;
    }
}
