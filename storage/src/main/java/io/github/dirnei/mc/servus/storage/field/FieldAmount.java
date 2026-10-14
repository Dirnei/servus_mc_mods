package io.github.dirnei.mc.servus.storage.field;

/**
 * A number of a Field Head the player types on the Stock tab: the harvest
 * limit, the fertilize level or the Keep in stock reserve. Pure Java.
 */
public enum FieldAmount {
    HARVEST_LIMIT(StockLevels.MAX),
    FERTILIZE_BELOW(StockLevels.MAX),
    RESERVE(FertilizePlan.MAX_RESERVE);

    private static final FieldAmount[] VALUES = values();

    private final int max;

    FieldAmount(int max) {
        this.max = max;
    }

    /** The amount with that ordinal, or null for an unknown id. */
    public static FieldAmount byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : null;
    }

    /** The largest value this amount takes. */
    public int max() {
        return max;
    }

    /** The value kept between 0 and {@link #max()}. */
    public int clamp(long value) {
        return (int) Math.max(0, Math.min(max, value));
    }

    /** The stock level this amount sets, or null for the reserve. */
    public StockLevels.Level level() {
        return switch (this) {
            case HARVEST_LIMIT -> StockLevels.Level.HARVEST;
            case FERTILIZE_BELOW -> StockLevels.Level.FERTILIZE;
            case RESERVE -> null;
        };
    }

    /**
     * The number typed into a field: empty or anything but digits counts as
     * 0, and a number too long for a long saturates at {@link Long#MAX_VALUE}.
     */
    public static long parse(String text) {
        if (text == null || text.isEmpty() || !text.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return 0;
        }
        long value = 0;
        for (int i = 0; i < text.length(); i++) {
            int digit = text.charAt(i) - '0';
            if (value > (Long.MAX_VALUE - digit) / 10) {
                return Long.MAX_VALUE;
            }
            value = value * 10 + digit;
        }
        return value;
    }
}
