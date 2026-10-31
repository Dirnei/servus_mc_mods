package io.github.dirnei.mc.servus.shuttles.field;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Copper corrosion of farm rails: the stages, aging by water used with
 * anti-corrosion upkeep, how far the water reaches along a field, and how a
 * scrape spreads over the rails. Pure Java.
 */
public final class FieldCorrosion {

    public static final int COPPER = 0;
    public static final int EXPOSED = 1;
    public static final int WEATHERED = 2;
    public static final int OXIDIZED = 3;
    public static final int CORRODED = 4;
    /** The number of stages, copper to corroded. */
    public static final int STAGES = 5;

    private static final String[] NAMES = {"copper", "exposed", "weathered", "oxidized", "corroded"};

    private FieldCorrosion() {
    }

    /** The result of counting water: the water still counted toward the next step, the upkeep items used, the steps no item held. */
    public record Aged(long counted, int itemsUsed, int steps) {
    }

    /**
     * The result of a scrape over rails in order.
     *
     * @param stages   the stage of each rail after the scrape, in the order given
     * @param progress the scrape progress of each rail after the scrape, in points
     * @param spent    the points the scrape used
     * @param scraped  the rails that went back a stage
     * @param left     the rails past copper that did not
     */
    public record Scrape(int[] stages, int[] progress, int spent, int scraped, int left) {
    }

    /** A stage kept in copper..corroded. */
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

    /** The lower case name of a stage. */
    public static String name(int stage) {
        return NAMES[clamp(stage)];
    }

    /**
     * Counts {@code added} mB of used water. Every {@code perStep} mB one aging
     * step is due: it uses one of the {@code items} upkeep items while any are
     * left, else it counts as a step that ages the wet rails. A {@code perStep}
     * of zero or less switches aging off.
     */
    public static Aged age(long counted, long added, long perStep, int items) {
        if (perStep <= 0) {
            return new Aged(Math.max(0, counted), 0, 0);
        }
        long total = Math.max(0, counted) + Math.max(0, added);
        long due = total / perStep;
        int used = (int) Math.min(Math.max(0, items), due);
        int steps = (int) Math.min(Integer.MAX_VALUE, due - used);
        return new Aged(total % perStep, used, steps);
    }

    /**
     * The rails the water reaches: breadth first from {@code start} over rails
     * that are not corroded. A corroded rail is dry and passes no water, so a
     * corroded start reaches none.
     */
    public static <T> Set<T> wet(T start, Function<T, ? extends Iterable<T>> neighbours, Predicate<T> corroded) {
        Set<T> wet = new LinkedHashSet<>();
        if (start == null || corroded.test(start)) {
            return wet;
        }
        Deque<T> open = new ArrayDeque<>();
        wet.add(start);
        open.add(start);
        while (!open.isEmpty()) {
            for (T next : neighbours.apply(open.poll())) {
                if (!wet.contains(next) && !corroded.test(next)) {
                    wet.add(next);
                    open.add(next);
                }
            }
        }
        return wet;
    }

    /**
     * Plans a scrape over rails in order. Each rail past copper goes back one
     * stage for {@code perRail} points less the progress it holds. When the
     * budget runs short, the rest stays on the next rail as progress. Copper
     * rails cost nothing. A negative {@code budget} is unlimited.
     */
    public static Scrape scrape(List<Integer> stages, List<Integer> progress, int perRail, int budget) {
        int size = stages.size();
        int[] newStages = new int[size];
        int[] newProgress = new int[size];
        int cost = Math.max(0, perRail);
        long left = budget < 0 ? Long.MAX_VALUE : budget;
        int spent = 0;
        int scraped = 0;
        int remaining = 0;
        for (int i = 0; i < size; i++) {
            int stage = clamp(stages.get(i));
            int held = i < progress.size() ? Math.max(0, progress.get(i)) : 0;
            newStages[i] = stage;
            newProgress[i] = stage > COPPER ? held : 0;
            if (stage == COPPER) {
                continue;
            }
            int need = Math.max(0, cost - held);
            if (left >= need) {
                left -= need;
                spent += need;
                newStages[i] = previous(stage);
                newProgress[i] = 0;
                scraped++;
            } else {
                newProgress[i] = held + (int) left;
                spent += (int) left;
                left = 0;
                remaining++;
            }
        }
        return new Scrape(newStages, newProgress, spent, scraped, remaining);
    }

    /** How far the counted water is toward the next step, in percent 0..99. */
    public static int progress(long counted, long perStep) {
        if (perStep <= 0 || counted <= 0) {
            return 0;
        }
        return (int) Math.min(99, counted * 100 / perStep);
    }
}
