package io.github.dirnei.mc.servus.storage.field;

import java.util.Collection;

/**
 * Copper corrosion of a field's farm rails: the stages, the stage of a join,
 * aging by water used with anti-corrosion upkeep, and the axe durability a
 * scrape needs. Pure Java.
 */
public final class FieldCorrosion {

    /** A placed rail that has not joined a field yet; drawn as fresh copper. */
    public static final int LOOSE = -1;
    public static final int COPPER = 0;
    public static final int EXPOSED = 1;
    public static final int WEATHERED = 2;
    public static final int OXIDIZED = 3;
    public static final int CORRODED = 4;

    private static final String[] NAMES = {"copper", "exposed", "weathered", "oxidized", "corroded"};

    private FieldCorrosion() {
    }

    /** The result of counting water: the stage after it, the water still counted toward the next step, the upkeep items used. */
    public record Aged(int stage, long counted, int itemsUsed) {
    }

    /** A stage kept in copper..corroded; loose counts as copper. */
    public static int clamp(int stage) {
        return Math.max(COPPER, Math.min(CORRODED, stage));
    }

    /** One stage older, at most corroded. */
    public static int next(int stage) {
        return Math.min(CORRODED, clamp(stage) + 1);
    }

    /** One stage younger, at least copper. */
    public static int previous(int stage) {
        return Math.max(COPPER, clamp(stage) - 1);
    }

    /** The lower case name of a stage, loose as copper. */
    public static String name(int stage) {
        return NAMES[clamp(stage)];
    }

    /**
     * The stage of a joined field: the serving head's stage when it knows one
     * ({@code headStage >= 0}), else the youngest stage the rails already have,
     * {@link #LOOSE} when none of them has one.
     */
    public static int join(int headStage, Collection<Integer> railStages) {
        if (headStage >= COPPER) {
            return clamp(headStage);
        }
        int youngest = LOOSE;
        for (int stage : railStages) {
            if (stage >= COPPER && (youngest == LOOSE || stage < youngest)) {
                youngest = clamp(stage);
            }
        }
        return youngest;
    }

    /**
     * Counts {@code added} mB of used water. Every {@code perStep} mB one aging
     * step is due: it uses one of the {@code items} upkeep items while any are
     * left, else moves the stage one older. A corroded field counts nothing.
     * A {@code perStep} of zero or less switches aging off.
     */
    public static Aged age(int stage, long counted, long added, long perStep, int items) {
        int now = clamp(stage);
        if (perStep <= 0 || now == CORRODED) {
            return new Aged(now, now == CORRODED ? 0 : Math.max(0, counted), 0);
        }
        long total = Math.max(0, counted) + Math.max(0, added);
        long steps = total / perStep;
        long rest = total % perStep;
        int used = 0;
        for (long i = 0; i < steps; i++) {
            if (used < items) {
                used++;
            } else {
                now = next(now);
                if (now == CORRODED) {
                    return new Aged(now, 0, used);
                }
            }
        }
        return new Aged(now, rest, used);
    }

    /** How far the counted water is toward the next step, in percent 0..99. */
    public static int progress(long counted, long perStep) {
        if (perStep <= 0 || counted <= 0) {
            return 0;
        }
        return (int) Math.min(99, counted * 100 / perStep);
    }

    /** The axe durability scraping a field of that many rails costs, never more than an int holds. */
    public static int axeDamage(int rails, int perRail) {
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, rails) * Math.max(0, perRail));
    }

    /** Whether an axe with that much durability left may take that much damage from a scrape. */
    public static boolean axeCanScrape(boolean damageable, int remaining, int damage) {
        return !damageable || remaining >= damage;
    }
}
